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

// [START functions_ocr_translate]

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.gson.Gson;
import com.google.pubsub.v1.PubsubMessage;
import java.io.IOException;
import java.util.logging.Logger;

public class OcrTranslateText implements BackgroundFunction<PubsubMessage> {
  // TODO<developer> set these environment variables
  private static final String PROJECT_ID = System.getenv("GCP_PROJECT");
  private static final String RESULTS_TOPIC_NAME = System.getenv("RESULT_TOPIC");

  private static final Logger LOGGER = Logger.getLogger(OcrTranslateText.class.getName());
  private static Publisher publisher;
  private static final Gson gson = new Gson();
  private static final String LOCATION_NAME = LocationName.of(PROJECT_ID, "global").toString();

  public OcrTranslateText() throws IOException {
    publisher = Publisher.newBuilder(RESULTS_TOPIC_NAME).build();
  }

  @Override
  public void accept(PubsubMessage pubsubMessage, Context context) {
    OcrTranslateApiMessage ocrMessage = OcrTranslateApiMessage.fromPubsubData(pubsubMessage);

    String targetLang = ocrMessage.getLang();
    LOGGER.info("Translating text into " + targetLang);

    // Translate text to target language
    String text = ocrMessage.getText();
    TranslateTextRequest request =
        TranslateTextRequest.newBuilder()
            .setParent(LOCATION_NAME.toString())
            .setMimeType("text/plain")
            .setTargetLanguageCode(targetLang)
            .addContents(text)
            .build();

    TranslateTextResponse response;
    try (TranslationServiceClient client = TranslationServiceClient.create()) {
      response = client.translateText(request);
    } catch (IOException e) {
      // Cast to RuntimeException
      throw new RuntimeException(e);
    }
    if (response == null || response.getTranslationsCount() == 0) {
      return;
    }

    String translatedText = response.getTranslations(0).getTranslatedText();
    LOGGER.info("Translated text: " + translatedText);

    // Send translated text to (subsequent) Pub/Sub topic
    String filename = ocrMessage.getFilename();
    OcrTranslateApiMessage message = new OcrTranslateApiMessage(
        translatedText, filename, targetLang);
    publisher.publish(message.toPubsubMessage());
    LOGGER.info("Text translated to " + targetLang);
  }
}
// [END functions_ocr_translate]
