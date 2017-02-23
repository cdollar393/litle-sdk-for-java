package com.litle.sdk;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.litle.sdk.generate.*;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by barnold on 2/15/17.
 */
public class StandaloneLoadTester {

    @Parameter(names = {"--numLoops", "-n"}, description = "Number of loops to execute for each thread. Default is 100")
    int numLoops = 100;

    @Parameter(names = {"--threads", "-t"}, description = "Number of threads to spin up concurrently. Default is 192")
    int numThreads = 192;

    @Parameter(names = {"--logging", "-l"}, description = "Enable command line logging. Default is 'false'")
    boolean logging = false;

    @Parameter(names = {"--fileName", "-f"},
            description = "File name to log output. Name is appended with run time, default is std out")
    String fileName = null;

    public static void main(String args[]) throws Exception {
        StandaloneLoadTester tester = new StandaloneLoadTester();
        new JCommander(tester, args);
        tester.run();
    }

    public void run() throws Exception {
        long now = System.currentTimeMillis();
        if (fileName != null) {
            System.setOut(new PrintStream(new File("/tmp/" + fileName + now)));
        }
        List<GetThread> threadList = new ArrayList<>();
        try {
            for (int i = 0; i < numThreads; i++) {
                GetThread myThread = new GetThread();
                myThread.setLoops(numLoops);
                if (logging) myThread.setLogging();
                threadList.add(myThread);
            }
            for (GetThread thread : threadList) {
                thread.start();
            }

            for (GetThread thread : threadList) {
                thread.join();
            }
        } catch (Exception e) {
            if (logging) {
                System.out.println("Exception in main...");
            }
        }
    }

    /**
     * inner class used to create multiple threads for simulating
     * a multi threaded environment or pool of LitleOnline objects
     */
    static class GetThread extends Thread {
        public LitleOnline litleOnline = new LitleOnline();
        private int loops = 400;
        long start = 0;
        boolean logging = false;

        public void setLoops(int loops) {
            this.loops = loops;
        }

        public void setLogging() {
            this.logging = true;
        }

        @Override
        public void run() {
            try {
                int count = 0;
                long start = 0;

                while (count < loops) {
                    Authorization authorization = new Authorization();
                    authorization.setReportGroup(String.valueOf(this.getId()));
                    authorization.setOrderId(String.valueOf(count));
                    authorization.setAmount(106L);
                    authorization.setOrderSource(OrderSourceType.ECOMMERCE);
                    authorization.setId("id");
                    CardType card = new CardType();
                    card.setType(MethodOfPaymentTypeEnum.VI);
                    card.setNumber("4100000000000000");
                    card.setExpDate("1210");
                    authorization.setCard(card);

                    start = System.currentTimeMillis();
                    AuthorizationResponse response = litleOnline.authorize(authorization);
                    long end = System.currentTimeMillis();

                    // if we have a mismatch, print the thread that went bad...
                    if (!String.valueOf(count).equals(response.getOrderId())
                            && !String.valueOf(this.getId()).equals(response.getReportGroup())) {
                        if (logging) {
                            System.out.println("******** Request and Response not in line! ********\nRequest: <" + count
                                    + "> Response: <" + response.getOrderId() + ">\nThread Request: <" + this.getId()
                                    + "> Response: <" + response.getReportGroup());
                        }
                    }

                    if (logging) {
                        System.out.println("Auth duration: <" + (end - start) + "> millis for thread: <" +
                                this.getId() + "> Completed loop # <" + count + ">\n");
                    }
                    count++;
                }
            } catch (Exception ex) {
                long duration = (System.currentTimeMillis() - start);
                if (logging) {
                    System.out.println("**Exception in thread <" + this.getId() + "> duration: <" + duration + "> millis.\n" +
                            "       Exception running auth <" + ex.getStackTrace() + "> message: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }
}



