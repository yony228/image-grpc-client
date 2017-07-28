package edu.nudt.das.image.grpc.client.prediction;

import com.google.protobuf.ByteString;
import com.google.protobuf.Int64Value;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.NettyChannelBuilder;
import org.apache.commons.lang3.StringUtils;
import org.tensorflow.framework.DataType;
import org.tensorflow.framework.TensorProto;
import org.tensorflow.framework.TensorShapeProto;
import tensorflow.serving.Model;
import tensorflow.serving.Predict;
import tensorflow.serving.PredictionServiceGrpc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * email: yony228@163.com
 * Created by yony on 17-7-7.
 */
public class ImagePredictClient {

        private final ManagedChannel channel;
        private final PredictionServiceGrpc.PredictionServiceBlockingStub blockingStub;

        // Initialize gRPC client
        public ImagePredictClient(String host, int port) {
                channel = NettyChannelBuilder.forAddress(host, port)
                        // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                        // needing certificates.
                        .usePlaintext(true)
                        .maxMessageSize(100 * 1024 * 1024)
                        .build();
                blockingStub = PredictionServiceGrpc.newBlockingStub(channel);
        }

        /**
         * predict using exist image
         *
         * @param modelName
         * @param signatureName
         * @param inputName
         * @param imagePath
         * @return
         * @throws IOException
         */
        public Map<String, TensorProto> predict_image(String modelName, String signatureName, String inputName, String imagePath) throws IOException {
                return predict_image(modelName, signatureName, inputName, -1, imagePath, null, -1);
        }

        /**
         * predict using inputStream
         *
         * @param modelName
         * @param signatureName
         * @param inputName
         * @param sis stream for predict
         * @return
         * @throws IOException
         */
        public Map<String, TensorProto> predict_image(String modelName, String signatureName, String inputName, InputStream sis) throws IOException {
                return predict_image(modelName, signatureName, inputName, -1, null, sis, -1);
        }

        /**
         * predict img classes
         *
         * @param modelName             must|                   model name defined by remote service
         * @param signatureName         must|                   signatureName defined by remote service
         * @param inputName             must|                   inputName defined by remote service
         * @param modelVersion          must|                   the version of model to predict img
         * @param imagePath             option|                 img local path for predict
         * @param sis                   option|                 img input stream for predict
         * @param duration              must|default:10s|       service out time,less the zero mean default
         * @return
         * @throws IOException
         */
        public Map<String, TensorProto> predict_image(String modelName, String signatureName, String inputName, long modelVersion, String imagePath, InputStream sis, long duration) throws IOException {
                TensorProto.Builder imageTensorBuilder = TensorProto.newBuilder();
                InputStream is = null;
                if(StringUtils.isNotBlank(imagePath)) {
                        is = new FileInputStream(imagePath);
                } else if (sis != null) {
                        is = sis;
                } else {
                        return null;
                }
//                int i = is.available();
                ByteString bs = ByteString.readFrom(is);
                is.close();

                imageTensorBuilder.addStringVal(bs);

                TensorShapeProto.Dim featuresDim1 = TensorShapeProto.Dim.newBuilder().setSize(1).build();
                TensorShapeProto keysShape = TensorShapeProto.newBuilder().addDim(featuresDim1).build();
                imageTensorBuilder.setDtype(DataType.DT_STRING).setTensorShape(keysShape);

                TensorProto imageTensorProto = imageTensorBuilder.build();

                // Generate gRPC request
                Model.ModelSpec.Builder builder = Model.ModelSpec.newBuilder().setName(modelName).setSignatureName(signatureName);
                builder = modelVersion > 0 ? builder.setVersion(Int64Value.newBuilder().setValue(modelVersion)) : builder;
                Model.ModelSpec modelSpec = builder.build();
                Predict.PredictRequest request = Predict.PredictRequest.newBuilder().setModelSpec(modelSpec).putInputs(StringUtils.isNotBlank(inputName)? inputName :"images", imageTensorProto).build();

                // Request gRPC server
                Predict.PredictResponse response;
                try {
                        response = blockingStub.withDeadlineAfter(duration > 0 ? duration : 10, TimeUnit.SECONDS).predict(request);
                        return response.getOutputsMap();
                } catch (StatusRuntimeException e) {
                        System.out.println("RPC failed:" + e.getStatus());
                        return null;
                }
        }

        /**
         * close
         *
         * @throws InterruptedException
         */
        public void shutdown() throws InterruptedException {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }

        public static void main(String[] args) {
                System.out.println("Start the predict client");

                String host = "127.0.0.1";//"10.107.20.15";
                int port = 19000;//28016;19000
                String modelName = "inception";
                String signatureName = "predict_images";
                String inputName = "images";
//                long modelVersion = 2;
                String filePath = "/home/yony/models/1.jpg";

                FileInputStream fis = null;
                // Run predict client to send request
                ImagePredictClient client = new ImagePredictClient(host, port);

                try {
                        fis = new FileInputStream(filePath);
                        //      filepath
                        //Map<String, TensorProto> outputs = client.predict_image(modelName, signatureName, inputName, filePath);
                        //      inputStream
                        Map<String, TensorProto> outputs = client.predict_image(modelName, signatureName, inputName, fis);
                        for (Map.Entry<String, TensorProto> entry : outputs.entrySet()) {
                                System.out.println("Response with the key: " + entry.getKey() + ", value: " + entry.getValue());
                        }
                } catch (Exception e) {
                        System.out.println(e);
                } finally {
                        try {
                                if(null != fis)
                                        fis.close();
                                client.shutdown();
                        } catch (Exception e) {
                                System.out.println(e);
                        }
                }

                System.out.println("End of predict client");
        }
}
