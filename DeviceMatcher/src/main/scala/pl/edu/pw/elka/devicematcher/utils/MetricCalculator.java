package pl.edu.pw.elka.devicematcher.utils;

import pl.edu.pw.elka.devicematcher.utils.Group;
import pl.edu.pw.elka.devicematcher.utils.MetricsUtils;
import scala.Int;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by szymon on 17.01.17.
 */
public class MetricCalculator {

    final static CountDownLatch latch1 = new CountDownLatch(1);
    final static CountDownLatch latch2 = new CountDownLatch(1);
    final static CountDownLatch latch3 = new CountDownLatch(1);
    final static CountDownLatch latch4 = new CountDownLatch(1);

    final static int[] metrics1 = new int[4];
    final static int[] metrics2 = new int[4];
    final static int[] metrics3 = new int[4];
    final static int[] metrics4 = new int[4];

    public static List<Integer> calculate(final List<Group> groups, final int min, final int max) {

        final int range = Math.round((min+max)/4);
        final int range1 = range;
        final int range2 = range*2;
        final int range3 = range*3;

        System.out.println("First thread from "+min+", until: "+range);
        System.out.println("Second thread from "+range1+", range "+(range1+range));
        System.out.println("Third thread from "+range2+", range "+(range2+range));
        System.out.println("Fourth thread from "+range3+", range "+(range3+max-3*range));

        Thread thread1 = new Thread() {
            @Override
            public void run(){
                List<Integer> list = MetricsUtils.getBasicMetrics(groups, min, range);
                for (int i=0; i<4; i++)
                    metrics1[i] = list.get(i);
                latch1.countDown();
            }
        };
        Thread thread2 = new Thread() {
            @Override
            public void run(){
                List<Integer> list = MetricsUtils.getBasicMetrics(groups, range1, range);
                for (int i=0; i<4; i++)
                    metrics2[i] = list.get(i);
                latch2.countDown();
            }
        };
        Thread thread3 = new Thread() {
            @Override
            public void run(){
                List<Integer> list = MetricsUtils.getBasicMetrics(groups, range2, range);
                for (int i=0; i<4; i++)
                    metrics3[i] = list.get(i);
                latch3.countDown();
            }
        };
        Thread thread4 = new Thread() {
            @Override
            public void run(){
                List<Integer> list = MetricsUtils.getBasicMetrics(groups, range3, max-3*range);
                for (int i=0; i<4; i++)
                    metrics4[i] = list.get(i);
                latch4.countDown();
            }
        };
        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();

        try {
            latch1.await();
            latch2.await();
            latch3.await();
            latch4.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<Integer> ret = new ArrayList<Integer>(4);
        ret.add(metrics1[0]+metrics2[0]+metrics3[0]+metrics4[0]);
        ret.add(metrics1[1]+metrics2[1]+metrics3[1]+metrics4[1]);
        ret.add(metrics1[2]+metrics2[2]+metrics3[2]+metrics4[2]);
        ret.add(metrics1[3]+metrics2[3]+metrics3[3]+metrics4[3]);
        return ret;
    }
}
