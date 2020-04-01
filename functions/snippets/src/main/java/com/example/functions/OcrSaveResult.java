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

// [START functions_ocr_save]

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.pubsub.v1.PubsubMessage;
import java.util.logging.Logger;

public class OcrSaveResult implements BackgroundFunction<PubsubMessage> {
  // TODO<developer> set this environment variable
  private static final String RESULT_BUCKET = System.getenv("RESULT_BUCKET");

  private static final Storage storage = StorageOptions.getDefaultInstance().getService();
  private static final Logger LOGGER = Logger.getLogger(OcrSaveResult.class.getName());

  @Override
  public void accept(PubsubMessage pubsubMessage, Context context) {
    OcrTranslateApiMessage ocrMessage = OcrTranslateApiMessage.fromPubsubData(pubsubMessage);

    String text = ocrMessage.getText();
    String filename = ocrMessage.getFilename();
    String lang = ocrMessage.getLang();

    LOGGER.info("Received request to save file " +  filename);

    // [START functions_ocr_rename]
    String newFileName = String.format("%s_to_%s.txt", filename, lang);
    // [END functions_ocr_rename]

    // Save file to RESULT_BUCKET with name newFileNaem
    LOGGER.info(String.format("Saving result to %s in bucket %s", newFileName, RESULT_BUCKET));
    BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(RESULT_BUCKET, newFileName)).build();
    storage.create(blobInfo, text.getBytes());
    LOGGER.info("File saved");
  }
}
// [END functions_ocr_save]
