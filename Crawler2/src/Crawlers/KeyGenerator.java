package Crawlers;
import java.security.SecureRandom;
import java.util.Scanner;


public class KeyGenerator {
	
	 public static byte[] generateRandomKey(int keySizeBytes) {
	        byte[] key = new byte[keySizeBytes];
	        SecureRandom secureRandom = new SecureRandom();
	        secureRandom.nextBytes(key);
	        return key;
	    }

	public static void main(String[] args) {
		 Scanner scanner = new Scanner(System.in);
	        System.out.print("Enter the size of the key in bytes: ");
	        int keySizeBytes = scanner.nextInt();
	        scanner.nextLine(); // Consume the remaining newline character

	        byte[] randomKey = generateRandomKey(keySizeBytes);
	        String hexKey = bytesToHex(randomKey);
	        System.out.println("Random Key: " + hexKey);
	    }
	
	 private static String bytesToHex(byte[] bytes) {
	        StringBuilder sb = new StringBuilder();
	        for (byte b : bytes) {
	            sb.append(String.format("%02x", b));
	        }
	        return sb.toString();
	
	}
}
