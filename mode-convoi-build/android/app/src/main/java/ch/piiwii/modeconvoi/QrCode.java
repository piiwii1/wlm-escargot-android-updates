package ch.piiwii.modeconvoi;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Encodeur QR minimal hors ligne, volontairement limité au format utilisé par Mode Convoi.
 * Version 2, correction L, mode octets UTF-8, masque 0. Capacité utile : 32 octets.
 * Aucun service externe n'est contacté pour générer le QR.
 */
public final class QrCode {
    public static final int SIZE = 25; // QR version 2
    private static final int DATA_CODEWORDS = 34;
    private static final int ECC_CODEWORDS = 10;

    private final boolean[][] modules = new boolean[SIZE][SIZE];
    private final boolean[][] function = new boolean[SIZE][SIZE];

    private QrCode() {}

    public static boolean[][] encodeText(String text) {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        if (data.length > 32) throw new IllegalArgumentException("QR Mode Convoi trop long");
        QrCode qr = new QrCode();
        qr.drawFunctionPatterns();
        byte[] codewords = makeCodewords(data);
        qr.drawCodewords(codewords);
        qr.applyMask0();
        qr.drawFormatBits(0);
        return qr.copyModules();
    }

    private boolean[][] copyModules() {
        boolean[][] out = new boolean[SIZE][SIZE];
        for (int y=0;y<SIZE;y++) System.arraycopy(modules[y],0,out[y],0,SIZE);
        return out;
    }

    private static byte[] makeCodewords(byte[] payload) {
        List<Integer> bits = new ArrayList<>();
        appendBits(bits, 0x4, 4);
        appendBits(bits, payload.length, 8);
        for (byte b : payload) appendBits(bits, b & 0xFF, 8);

        int capacity = DATA_CODEWORDS * 8;
        appendBits(bits, 0, Math.min(4, capacity - bits.size()));
        while ((bits.size() & 7) != 0) bits.add(0);

        byte[] data = new byte[DATA_CODEWORDS];
        int count = bits.size() / 8;
        for (int i=0;i<count;i++) {
            int val=0;
            for (int j=0;j<8;j++) val=(val<<1)|bits.get(i*8+j);
            data[i]=(byte)val;
        }
        for (int i=count;i<DATA_CODEWORDS;i++) data[i]=(byte)(((i-count)&1)==0 ? 0xEC : 0x11);

        byte[] divisor = reedSolomonDivisor(ECC_CODEWORDS);
        byte[] ecc = reedSolomonRemainder(data, divisor);
        byte[] result = new byte[DATA_CODEWORDS + ECC_CODEWORDS];
        System.arraycopy(data,0,result,0,data.length);
        System.arraycopy(ecc,0,result,data.length,ecc.length);
        return result;
    }

    private static void appendBits(List<Integer> out, int value, int len) {
        if (len < 0 || len > 31 || (len < 31 && (value >>> len) != 0)) throw new IllegalArgumentException();
        for (int i=len-1;i>=0;i--) out.add((value >>> i) & 1);
    }

    private void drawFunctionPatterns() {
        for (int i=0;i<SIZE;i++) {
            setFunction(6,i,(i&1)==0);
            setFunction(i,6,(i&1)==0);
        }
        drawFinder(3,3);
        drawFinder(SIZE-4,3);
        drawFinder(3,SIZE-4);
        drawAlignment(18,18);
        drawFormatBits(0);
    }

    private void drawFinder(int cx, int cy) {
        for (int dy=-4;dy<=4;dy++) for (int dx=-4;dx<=4;dx++) {
            int x=cx+dx,y=cy+dy;
            if (0<=x && x<SIZE && 0<=y && y<SIZE) {
                int dist=Math.max(Math.abs(dx),Math.abs(dy));
                setFunction(x,y,dist!=2 && dist!=4);
            }
        }
    }

    private void drawAlignment(int cx, int cy) {
        for (int dy=-2;dy<=2;dy++) for (int dx=-2;dx<=2;dx++)
            setFunction(cx+dx,cy+dy,Math.max(Math.abs(dx),Math.abs(dy))!=1);
    }

    private void drawFormatBits(int mask) {
        int data = (1 << 3) | mask;
        int rem = data;
        for (int i=0;i<10;i++) rem=(rem<<1) ^ (((rem>>>9)&1) * 0x537);
        int bits=((data<<10)|rem)^0x5412;

        for (int i=0;i<=5;i++) setFunction(8,i,getBit(bits,i));
        setFunction(8,7,getBit(bits,6));
        setFunction(8,8,getBit(bits,7));
        setFunction(7,8,getBit(bits,8));
        for (int i=9;i<15;i++) setFunction(14-i,8,getBit(bits,i));

        for (int i=0;i<8;i++) setFunction(SIZE-1-i,8,getBit(bits,i));
        for (int i=8;i<15;i++) setFunction(8,SIZE-15+i,getBit(bits,i));
        setFunction(8,SIZE-8,true);
    }

    private void drawCodewords(byte[] data) {
        int bitIndex=0;
        for (int right=SIZE-1;right>=1;right-=2) {
            if (right==6) right=5;
            for (int vert=0;vert<SIZE;vert++) {
                boolean upward=((right+1)&2)==0;
                int y=upward ? SIZE-1-vert : vert;
                for (int j=0;j<2;j++) {
                    int x=right-j;
                    if (!function[y][x] && bitIndex < data.length*8) {
                        int b=data[bitIndex>>>3]&0xFF;
                        modules[y][x]=((b >>> (7-(bitIndex&7))) & 1) != 0;
                        bitIndex++;
                    }
                }
            }
        }
        if (bitIndex != data.length*8) throw new IllegalStateException("QR data placement incomplete");
    }

    private void applyMask0() {
        for (int y=0;y<SIZE;y++) for (int x=0;x<SIZE;x++)
            if (!function[y][x] && ((x+y)&1)==0) modules[y][x]=!modules[y][x];
    }

    private void setFunction(int x,int y,boolean black) {
        modules[y][x]=black;
        function[y][x]=true;
    }
    private static boolean getBit(int x,int i) { return ((x>>>i)&1)!=0; }

    private static byte[] reedSolomonDivisor(int degree) {
        byte[] result=new byte[degree];
        result[degree-1]=1;
        int root=1;
        for (int i=0;i<degree;i++) {
            for (int j=0;j<degree;j++) {
                result[j]=(byte)multiply(result[j]&0xFF,root);
                if (j+1<degree) result[j]^=result[j+1];
            }
            root=multiply(root,0x02);
        }
        return result;
    }

    private static byte[] reedSolomonRemainder(byte[] data, byte[] divisor) {
        byte[] result=new byte[divisor.length];
        for (byte b:data) {
            int factor=(b&0xFF)^(result[0]&0xFF);
            System.arraycopy(result,1,result,0,result.length-1);
            result[result.length-1]=0;
            for (int i=0;i<result.length;i++) result[i]^=(byte)multiply(divisor[i]&0xFF,factor);
        }
        return result;
    }

    private static int multiply(int x,int y) {
        int z=0;
        for (int i=7;i>=0;i--) {
            z=(z<<1)^(((z>>>7)&1)*0x11D);
            z^=((y>>>i)&1)*x;
        }
        return z & 0xFF;
    }
}
