import java.math.BigInteger;

/**
 *
 * @author Nika
 */
public class ForDemo {

    int i = 0;

    byte b = 0;

    short s = 0;

    double d = 0;

    float f = 0;

    public ForDemo() {
        for (int i = 0; i <= 100000; i++) {
            System.out.println("" + i);
        }
    }

    public static void main(String[] args) {
        new ForDemo();
    }
}
