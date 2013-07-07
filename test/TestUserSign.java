import java.math.BigInteger;
import java.security.*;

public class TestUserSign {




    public static byte[] signEC(byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

        keyGen.initialize(256, random);

        KeyPair pair = keyGen.generateKeyPair();
        PrivateKey priv = pair.getPrivate();
        PublicKey pub = pair.getPublic();

        /*
         * Create a Signature object and initialize it with the private key
         */

        Signature dsa = Signature.getInstance("SHA1withECDSA");

        dsa.initSign(priv);

        dsa.update(data);

        /*
         * Now that all the data to be signed has been read in, generate a
         * signature for it
         */

        return dsa.sign();
    }


    public static void main(String[] args) throws Exception {


        String str = "This is string to sign";
        byte[] strByte = str.getBytes("UTF-8");

        byte[] realSig = signEC(strByte);
        System.out.println("Signature:\n" + new BigInteger(1, realSig).toString(16));
    }



}
