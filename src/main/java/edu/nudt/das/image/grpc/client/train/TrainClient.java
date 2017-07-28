package edu.nudt.das.image.grpc.client.train;

import edu.nudt.das.image.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;

import java.util.concurrent.TimeUnit;

/**
 * email: yony228@163.com
 * Created by yony on 17-6-28.
 */
public class TrainClient {
        private final ManagedChannel channel, channel_new;
        private final FormatDataGrpc.FormatDataBlockingStub blockingStub;
        private final TrainServiceGrpc.TrainServiceBlockingStub trainStub;

        public TrainClient(String host, int port) {
                channel = ManagedChannelBuilder.forAddress(host, port)
                        .usePlaintext(true)
                        .build();
                channel_new = NettyChannelBuilder.forAddress(host, port)
                        .usePlaintext(true)
                        .maxMessageSize(100 * 1024 * 1024)
                        .build();

                blockingStub = FormatDataGrpc.newBlockingStub(channel);
                trainStub = TrainServiceGrpc.newBlockingStub(channel_new);
        }

        public void shutdown() throws InterruptedException {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                channel_new.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }

        public void FormatFiles(String name) {
                Data request = Data.newBuilder().setText(name).build();
                Data response = blockingStub.doFormat(request);
                System.out.println(response.getText());
        }

        public TrainResponse train(String trainNo, long trainStep, int trainTestPercent, int trainShardNum) {
                TrainRequest request = TrainRequest.newBuilder()
                        .setTrainNo(trainNo)
                        .setTrainStep(trainStep)
                        .setTrainTestPercent(trainTestPercent)
                        .setTrainShardNum(trainShardNum)
                        .build();
                return trainStub.doTrain(request);

        }

        public static void main(String[] args) throws InterruptedException {
                TrainClient client = new TrainClient("10.107.20.17", 50051);//172.17.0.2 10.107.20.17
//                client.FormatFiles("T201706221606037305");
                TrainResponse response = client.train("T201706221606037305", 10l, 0, 1);
                System.out.println("key:" + response.getKey());
                System.out.println("message:" + response.getMessage());
        }
}