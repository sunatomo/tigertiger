import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.util.Arrays;
import java.util.Comparator;


public class TigerTiger2 {
    final static int num = 20;

    static class Dot {
        short x;
        short y;
        short c;
    }

    static class Obj {
        Dot[] l = new Dot[num];
        Dot[] d = new Dot[num];
        short b;
        int h;
        int w;
        int thresh;

        Obj(String name, int th) {
            thresh = th;
            for (int i = 0; i < num; ++i) {
                l[i] = new Dot();
                d[i] = new Dot();
                d[i].c = 256;
            }
            Mat img = Imgcodecs.imread(name);
            h = img.rows();
            w = img.cols();
            byte[] bgr = new byte[3];
            for (short r = 0; r < h; ++r) {
                for (short c = 0; c < w; ++c) {
                    img.get(r, c, bgr);
                    int a = (U(bgr[0]) + U(bgr[1]) + U(bgr[2])) / 3;
                    if (a == 0) continue;
                    Arrays.sort(l, Comparator.comparingInt(o -> o.c));
                    Arrays.sort(d, Comparator.comparingInt(o -> o.c));
                    if (l[0].c < a) {
                        l[0].c = (short) a;
                        l[0].x = c;
                        l[0].y = r;
                    }
                    if (d[num - 1].c > a) {
                        d[num - 1].c = (short) a;
                        d[num - 1].x = c;
                        d[num - 1].y = r;
                    }
                }
            }
            Arrays.sort(l, Comparator.comparingInt(o -> o.c));
            Arrays.sort(d, Comparator.comparingInt(o -> o.c));
            b = (short) ((l[0].c + d[num - 1].c) / 2);
        }

        int detect(byte[] data, byte[] mask, int x, int y) {
            int badl = 0;
            for (Dot d : l) {
                int i = (y + d.y) * 640 + x + d.x;
                if (data[i] < b) badl++;
                if (badl > thresh) return -1;
            }
            int badd = 0;
            for (Dot d : d) {
                int i = (y + d.y) * 640 + x + d.x;
                if (mask[i] > b) badd++;
                if (badd > thresh) return -1;
            }
            return badd + badl;
        }

        int detectRev(byte[] data, byte[] mask, int x, int y) {
            int badl = 0;
            for (Dot d : l) {
                int i = (y + d.y) * 640 + x + w - d.x;
                if (data[i] < b) badl++;
                if (badl > thresh) return -1;
            }
            int badd = 0;
            for (Dot d : d) {
                int i = (y + d.y) * 640 + x + w - d.x;
                if (mask[i] > b) badd++;
                if (badd > thresh) return -1;
            }
            return badd + badl;
        }
    }

    static byte[] data = new byte[640 * 480];
    static byte[] mask = new byte[640 * 480];
    static byte[] backl = new byte[640 * 480];
    static byte[] backd = new byte[640 * 480];

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static int U(byte b) {
        return b & 255;
    }

    public static void main(String[] args) {
        Obj coin = new Obj("coin2.png", 4);
        Obj item = new Obj("item.png", 4);
        Obj block = new Obj("block2.png", 2);
        Obj shark = new Obj("shark2.png", 2);
        Obj main2 = new Obj("main2.png", 4);
        Obj main3 = new Obj("main3.png", 4);
        Obj main5 = new Obj("main5.png", 4);
        Obj toge = new Obj("toge.png", 5);
        Obj boss = new Obj("boss.png", 2);
        Obj kame = new Obj("kame.png", 2);
        Obj kurage = new Obj("kurage.png", 2);
        Obj treasure = new Obj("treasure.png", 2);
        Obj[] enemy = {
                shark,
                kame,
                kurage,
                boss
        };
        Obj[] fixed = {
                block,
                toge
        };
        Obj[] score = {
                coin,
                item,
                treasure
        };
        VideoCapture video = new VideoCapture(0);
        Mat mat_src = new Mat();
        Mat org = mat_src.clone();
        Mat otsu = mat_src.clone();
        HighGui.namedWindow("hoge");
        HighGui.namedWindow("origin");

        do {
            video.read(org);
            Imgproc.cvtColor(org, mat_src, Imgproc.COLOR_BGR2GRAY);
            Imgproc.threshold(mat_src, otsu, 0, 255, Imgproc.THRESH_OTSU);
            mat_src.get(0, 0, data);
            otsu.get(0, 0, mask);
            for (int y = 1; y < 480 - 1; ++y)
                for (int x = 1; x < 640 - 1; ++x) {
                    int i = y * 640 + x;
                    if (mask[i] == 0) {
                        backl[i] = 0;
                    } else {
                        int a = (U(data[i - 1]) + U(data[i + 1]) + U(data[i - 640]) + U(data[i + 640])) / 4;
                        int c = U(data[i]) - a;
                        if (c < 0) c = 0;
                        if (c >= 256) c = 255;
                        backl[i] = (byte) c;
                        backl[i - 1] = U(backl[i - 1]) < c ? (byte) c : backl[i - 1];
                        backl[i - 640] = U(backl[i - 640]) < c ? (byte) c : backl[i - 640];
                        backd[i] = (byte) c;
                        backd[i - 1] = U(backd[i - 1]) > c ? (byte) c : backd[i - 1];
                        backd[i - 640] = U(backd[i - 640]) > c ? (byte) c : backd[i - 640];
                        mask[i] = (byte) c;
                    }

                }

            for (int y = 80; y < 360; ++y)
                for (int x = 190; x < 410; ++x) {
                    for (Obj obj : enemy) {
                        if (obj.detect(backl, backd, x, y) >= 0 || obj.detectRev(backl, backd, x, y) >= 0)
                            Imgproc.rectangle(org, new Rect(x, y, obj.w, obj.h), new Scalar(0, 0, 255), 1);
                    }
                    for (Obj obj : fixed) {
                        if (obj.detect(backl, backd, x, y) >= 0)
                            Imgproc.rectangle(org, new Rect(x, y, obj.w, obj.h), new Scalar(255, 255, 255), 1);
                    }
                    for (Obj obj : score) {
                        if (obj.detect(backl, backd, x, y) >= 0)
                            Imgproc.rectangle(org, new Rect(x, y, obj.w, obj.h), new Scalar(0, 255, 255), 1);
                    }
                    if (main2.detect(backl, backd, x, y) >= 0 || main2.detectRev(backl, backd, x, y) >= 0
                            || main3.detect(backl, backd, x, y) >= 0 || main5.detect(backl, backd, x, y) >= 0)
                        Imgproc.rectangle(org, new Rect(x, y, main2.w, main2.h), new Scalar(255, 0, 0), 1);
                }
            mat_src.put(0, 0, mask);
            HighGui.imshow("origin", org);
            HighGui.imshow("hoge", mat_src);
            HighGui.waitKey(15);
        } while (true);
    }
}
