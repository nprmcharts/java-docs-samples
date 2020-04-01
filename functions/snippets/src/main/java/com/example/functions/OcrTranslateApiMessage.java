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

// [START functions_ocr_translate_pojo]
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.util.Base64;

// Object for storing OCR translation requests
public class OcrTranslateApiMessage {
  private String text;
  private String filename;
  private String lang;

  private static final Gson gson = new Gson();

  public OcrTranslateApiMessage(String text, String filename, String lang) {
    this.text = text;
    this.filename = filename;
    this.lang = lang;
  }

  public String getText() {
    return text;
  }

  public String getFilename() {
    return filename;
  }

  public String getLang() {
    return lang;
  }

  public static OcrTranslateApiMessage fromPubsubData(PubsubMessage message)
      throws IllegalArgumentException {
    String jsonStr = new String(Base64.getDecoder().decode(message.getData().toByteArray()));
    OcrTranslateApiMessage ocrMessage = gson.fromJson(jsonStr, OcrTranslateApiMessage.class);

    // Get + verify parameters
    String text = ocrMessage.getText();
    String filename = ocrMessage.getFilename();
    String targetLang = ocrMessage.getLang();
    if (text == null) {
      throw new IllegalArgumentException("Missing text parameter");
    }
    if (filename == null) {
      throw new IllegalArgumentException("Missing filename parameter");
    }
    if (targetLang == null) {
      throw new IllegalArgumentException("Missing lang parameter");
    }

    return ocrMessage;
  }

  public PubsubMessage toPubsubMessage() {
    ByteString data = ByteString.copyFromUtf8(gson.toJson(this));
    return PubsubMessage.newBuilder().setData(data).build();
  }
}
// [END functions_ocr_translate_pojo]