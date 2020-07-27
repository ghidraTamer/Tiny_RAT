import com.sun.security.ntlm.Client;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Handler;

class Main {


    public static void main(String args[]) {
        boolean created = new File("new folder").mkdir();
        System.out.println(created);

    }


}
