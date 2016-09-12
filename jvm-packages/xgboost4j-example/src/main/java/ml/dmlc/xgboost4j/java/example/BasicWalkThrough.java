/*
 Copyright (c) 2014 by Contributors

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package ml.dmlc.xgboost4j.java.example;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import ml.dmlc.xgboost4j.java.example.util.DataLoader;

/**
 * a simple example of java wrapper for xgboost
 *
 * @author hzx
 */
public class BasicWalkThrough {
  public static boolean checkPredicts(float[][] fPredicts, float[][] sPredicts) {
    if (fPredicts.length != sPredicts.length) {
      return false;
    }

    for (int i = 0; i < fPredicts.length; i++) {
      if (!Arrays.equals(fPredicts[i], sPredicts[i])) {
        return false;
      }
    }

    return true;
  }


  public static void main(String[] args) throws IOException, XGBoostError {
    // load file from text file, also binary buffer generated by xgboost4j
    DMatrix trainMat = new DMatrix("../../demo/data/agaricus.txt.train");
    DMatrix testMat = new DMatrix("../../demo/data/agaricus.txt.test");

    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("eta", 1.0);
    params.put("max_depth", 2);
    params.put("silent", 1);
    params.put("objective", "binary:logistic");


    HashMap<String, DMatrix> watches = new HashMap<String, DMatrix>();
    watches.put("train", trainMat);
    watches.put("test", testMat);

    //set round
    int round = 2;

    //train a boost model
    Booster booster = XGBoost.train(trainMat, params, round, watches, null, null);

    //predict
    float[][] predicts = booster.predict(testMat);

    //save model to modelPath
    File file = new File("./model");
    if (!file.exists()) {
      file.mkdirs();
    }

    String modelPath = "./model/xgb.model";
    booster.saveModel(modelPath);

    //dump model
    booster.getModelDump("./model/dump.raw.txt", false);

    //dump model with feature map
    booster.getModelDump("../../demo/data/featmap.txt", false);

    //save dmatrix into binary buffer
    testMat.saveBinary("./model/dtest.buffer");

    //reload model and data
    Booster booster2 = XGBoost.loadModel("./model/xgb.model");
    DMatrix testMat2 = new DMatrix("./model/dtest.buffer");
    float[][] predicts2 = booster2.predict(testMat2);


    //check the two predicts
    System.out.println(checkPredicts(predicts, predicts2));

    System.out.println("start build dmatrix from csr sparse data ...");
    //build dmatrix from CSR Sparse Matrix
    DataLoader.CSRSparseData spData = DataLoader.loadSVMFile("../../demo/data/agaricus.txt.train");

    DMatrix trainMat2 = new DMatrix(spData.rowHeaders, spData.colIndex, spData.data,
            DMatrix.SparseType.CSR);
    trainMat2.setLabel(spData.labels);

    //specify watchList
    HashMap<String, DMatrix> watches2 = new HashMap<String, DMatrix>();
    watches2.put("train", trainMat2);
    watches2.put("test", testMat2);
    Booster booster3 = XGBoost.train(trainMat2, params, round, watches2, null, null);
    float[][] predicts3 = booster3.predict(testMat2);

    //check predicts
    System.out.println(checkPredicts(predicts, predicts3));
  }
}
