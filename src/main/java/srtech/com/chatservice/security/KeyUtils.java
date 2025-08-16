package srtech.com.chatservice.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Objects;

@Component
public class KeyUtils {
    @Autowired
    Environment environment;
    @Value("${ACCESS_TOKEN_PRIVATE_KEY_PATH}")
    private String accessTokenPrivateKey;
    @Value("${ACCESS_TOKEN_PUBLIC_KEY_PATH}")
    private String accessTokenPublicKey;
    @Value("${REFRESH_TOKEN_PRIVATE_KEY_PATH}")
    private String refreshTokenPrivateKey;
    @Value("${REFRESH_TOKEN_PUBLIC_KEY_PATH}")
    private String refreshTokenPublicKey;

    KeyPair _accessTokenKeyPair;
    KeyPair _refreshTokenKeyPair;

    private KeyPair getAccessTokenKeyPair(){
        if(Objects.isNull(_accessTokenKeyPair)){
            _accessTokenKeyPair = getKeyPair(accessTokenPublicKey,accessTokenPrivateKey);
        }
        return _accessTokenKeyPair;

    }
    private KeyPair getRefreshTokenKeyPair(){
        if(Objects.isNull(_refreshTokenKeyPair)){
            _refreshTokenKeyPair = getKeyPair(refreshTokenPublicKey, refreshTokenPrivateKey);
        }
        return _refreshTokenKeyPair;
    }
    private KeyPair getKeyPair(String publicKeyPath , String privateKeyPath ){
        KeyPair keyPair;
        File publicKeyFile = new File(publicKeyPath);
        File privateKeyFile = new File(privateKeyPath);
        if (publicKeyFile.exists() && privateKeyFile.exists()){
            try{
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                // read key from file and create a public key with encryption
                byte[] publicKeyBytes  = Files.readAllBytes(publicKeyFile.toPath());
                //X509 -> used for public key
                EncodedKeySpec publicKeySpec =  new X509EncodedKeySpec(publicKeyBytes);
                PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);


                // create a private key
                byte[] privateKeyBytes = Files.readAllBytes(privateKeyFile.toPath());
                PKCS8EncodedKeySpec privateKeySpec  = new PKCS8EncodedKeySpec(privateKeyBytes);
                PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

                keyPair = new KeyPair(publicKey , privateKey);
                return keyPair;


            }catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException ex ){
                throw new RuntimeException( ex );
            }

        }else {

            if(Arrays.asList(environment.getActiveProfiles()).contains("prod")){
//                throw new RuntimeException("public and private key doesn't exist !");
            }
        }


        File directory = new File("access-refresh-token-keys");
        if(!directory.exists()){
            directory.mkdirs();
        }

        try{
            // we are going to generate a keypair
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();


            try(FileOutputStream fos = new FileOutputStream(publicKeyPath)){
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyPair.getPublic().getEncoded());
                fos.write(keySpec.getEncoded());
            }
            try(FileOutputStream fos = new FileOutputStream(privateKeyFile)){
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyPair.getPrivate().getEncoded());
                fos.write(keySpec.getEncoded());
            }

        }catch (NoSuchAlgorithmException | IOException ex ){
            throw new RuntimeException(ex);
        }

        return keyPair;

    }
    // get public key and private key for the access token and refresh token for other class to use
    public RSAPublicKey getAccessTokenPublicKey(){
        return (RSAPublicKey) getAccessTokenKeyPair().getPublic();
    }
    public RSAPrivateKey getAccessTokenPrivateKey(){
        return (RSAPrivateKey) getAccessTokenKeyPair().getPrivate();
    }
    public RSAPrivateKey getRefreshTokenPrivateKey(){
        return (RSAPrivateKey) getRefreshTokenKeyPair().getPrivate();
    }
    public RSAPublicKey getRefreshTokenPublicKey(){
        return (RSAPublicKey)getRefreshTokenKeyPair().getPublic();
    }


}
