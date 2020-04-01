/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.functions;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.testing.TestLogHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class OcrTests {
  private static String FUNCTIONS_BUCKET = "nodejs-docs-samples-tests";
  private static String RESULT_BUCKET = System.getenv("RESULT_BUCKET");

  private static final Logger PROCESS_IMAGE_LOGGER = Logger.getLogger(
      OcrProcessImage.class.getName());
  private static final Logger SAVE_RESULT_LOGGER = Logger.getLogger(OcrSaveResult.class.getName());
  private static final Logger TRANSLATE_TEXT_LOGGER = Logger.getLogger(
      OcrTranslateText.class.getName());

  private static final TestLogHandler LOG_HANDLER = new TestLogHandler();

  private static final Gson gson = new Gson();

  private static final Storage storage = StorageOptions.getDefaultInstance().getService();

  @BeforeClass
  public static void beforeClass() {
    PROCESS_IMAGE_LOGGER.addHandler(LOG_HANDLER);
    SAVE_RESULT_LOGGER.addHandler(LOG_HANDLER);
    TRANSLATE_TEXT_LOGGER.addHandler(LOG_HANDLER);
  }

  @After
  public void afterTest() {
    LOG_HANDLER.clear();
  }

  @Test(expected = IllegalArgumentException.class)
  public void functionsOcrProcess_shouldValidateParams() throws IOException {
    new OcrProcessImage().accept(new GcsEvent(), null);
  }

  @Test
  public void functionsOcrProcess_shouldDetectText() throws IOException {
    GcsEvent gcsEvent = new GcsEvent();
    gcsEvent.bucket = FUNCTIONS_BUCKET;
    gcsEvent.name = "wakeupcat.jpg";

    new OcrProcessImage().accept(gcsEvent, null);

    List<LogRecord> logs = LOG_HANDLER.getStoredLogRecords();
    assertThat(logs.get(1).getMessage()).contains("Extracted text from image: Wake up human!");
    assertThat(logs.get(2).getMessage()).contains("Detected language en for file wakeupcat.jpg");
  }

  @Test(expected = IllegalArgumentException.class)
  public void functionsOcrTranslate_shouldValidateParams() throws IOException {
    ByteString base64data = ByteString.copyFrom(Base64.getEncoder().encode("{}".getBytes()));
    PubsubMessage message = PubsubMessage.newBuilder().setData(base64data).build();

    new OcrTranslateText().accept(message, null);
  }

  @Test
  public void functionsOcrTranslate_shouldTranslateText() throws IOException {
    String text = "Wake up human!";
    String filename = "wakeupcat.jpg";
    String lang = "es";

    JsonObject dataJson = new JsonObject();
    dataJson.addProperty("text", text);
    dataJson.addProperty("filename", filename);
    dataJson.addProperty("lang", lang);

    byte[] dataBytes = Base64.getEncoder().encode(gson.toJson(dataJson).getBytes());
    PubsubMessage message = PubsubMessage
        .newBuilder()
        .setData(ByteString.copyFrom(dataBytes))
        .build();

    new OcrTranslateText().accept(message, null);

    List<LogRecord> logs = LOG_HANDLER.getStoredLogRecords();
    assertThat(logs.get(1).getMessage()).contains("¡Despierta humano!");
    assertThat(logs.get(2).getMessage()).isEqualTo("Text translated to es");
  }

  @Test(expected = IllegalArgumentException.class)
  public void functionsOcrSave_shouldValidateParams() throws IOException {
    ByteString base64data = ByteString.copyFrom(Base64.getEncoder().encode("{}".getBytes()));
    PubsubMessage message = PubsubMessage.newBuilder().setData(base64data).build();

    new OcrSaveResult().accept(message, null);
  }

  @Test
  public void functionsOcrSave_shouldPublishTranslatedText() throws IOException {
    String text = "Wake up human!";
    String filename = "wakeupcat.jpg";
    String lang = "es";

    JsonObject dataJson = new JsonObject();
    dataJson.addProperty("text", text);
    dataJson.addProperty("filename", filename);
    dataJson.addProperty("lang", lang);

    byte[] dataBytes = Base64.getEncoder().encode(gson.toJson(dataJson).getBytes());
    PubsubMessage message = PubsubMessage
        .newBuilder()
        .setData(ByteString.copyFrom(dataBytes))
        .build();

    new OcrSaveResult().accept(message, null);

    String resultFilename = "wakeupcat.jpg_to_es.txt";

    // Check log messages
    List<LogRecord> logs = LOG_HANDLER.getStoredLogRecords();
    String expectedMessage = String.format(
        "Saving result to %s in bucket %s", resultFilename, RESULT_BUCKET);
    assertThat(LOG_HANDLER.getStoredLogRecords().get(1).getMessage()).isEqualTo(expectedMessage);

    // Check for (recently) written file
    // (Using timestamps instead of exists() saves us a deletion step)
    BlobInfo resultBlob = storage.get(RESULT_BUCKET, resultFilename);
    Long fiveMinutesAgo = System.currentTimeMillis() - 60000 * 5;
    assertThat(resultBlob).isNotNull();
    assertThat(resultBlob.getCreateTime()).isAtLeast(fiveMinutesAgo);
  }
}
