package org.sufficientlysecure.keychain.securitytoken;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sufficientlysecure.keychain.KeychainTestRunner;

import static junit.framework.Assert.assertEquals;


@RunWith(KeychainTestRunner.class)
@Ignore("Only for reference right now")
public class SecurityTokenConnectionCompatTest {
    private byte[] encryptedSessionKey;
    private OpenPgpCommandApduFactory openPgpCommandApduFactory;

    @Before
    public void setUp() throws Exception {
        encryptedSessionKey = Hex.decode("07ff7b9ff36f70da1fe7a6b59168c24a7e5b48a938c4f970de46524a06ebf4a9175a9737cf2e6f30957110b31db70e9a2992401b1d5e99389f976356f4e3a28ff537362e7ce14b81200e21d4f0e77d46bd89f3a54ca06062289148a59387488ac01d30d2baf58e6b35e32434720473604a9f7d5083ca6d40e4a2dadedd68033a4d4bbdb06d075d6980c0c0ca19078dcdfb9d8cbcb34f28d0b968b6e09eda0e1d3ab6b251eb09f9fb9d9abfeaf9010001733b9015e9e4b6c9df61bbc76041f439d1273e41f5d0e8414a2b8d6d4c7e86f30b94cfba308b38de53d694a8ca15382301ace806c8237641b03525b3e3e8cbb017e251265229bcbb0da5d5aeb4eafbad9779");

        openPgpCommandApduFactory = new OpenPgpCommandApduFactory();
    }

    /* we have a report of breaking compatibility on some earlier version.
        this test checks what was sent in that version to what we send now.
    // see https://github.com/open-keychain/open-keychain/issues/2049
    // see https://github.com/open-keychain/open-keychain/commit/ee8cd3862f65de580ed949bc838628610e22cd98
    */

    @Test
    public void testPrePostEquals() {
        List<String> preApdus = decryptPre_ee8cd38();
        List<String> postApdus = decryptNow();

        assertEquals(preApdus, postApdus);
    }

    public List<String> decryptPre_ee8cd38() {
        final int MAX_APDU_DATAFIELD_SIZE = 254;

        int offset = 1; // Skip first byte
        List<String> apduData = new ArrayList<>();

        // Transmit
        while (offset < encryptedSessionKey.length) {
            boolean isLastCommand = offset + MAX_APDU_DATAFIELD_SIZE < encryptedSessionKey.length;
            String cla = isLastCommand ? "10" : "00";

            int len = Math.min(MAX_APDU_DATAFIELD_SIZE, encryptedSessionKey.length - offset);
            apduData.add(cla + "2a8086" + Hex.toHexString(new byte[]{(byte) len})
                    + Hex.toHexString(encryptedSessionKey, offset, len));

            offset += MAX_APDU_DATAFIELD_SIZE;
        }

        return apduData;
    }

    public List<String> decryptNow() {
        int mpiLength = ((((encryptedSessionKey[0] & 0xff) << 8) + (encryptedSessionKey[1] & 0xff)) + 7) / 8;
        byte[] psoDecipherPayload = new byte[mpiLength + 1];
        psoDecipherPayload[0] = (byte) 0x00;
        System.arraycopy(encryptedSessionKey, 2, psoDecipherPayload, 1, mpiLength);

        CommandApdu command = openPgpCommandApduFactory.createDecipherCommand(psoDecipherPayload);
        List<CommandApdu> chainedApdus = openPgpCommandApduFactory.createChainedApdus(command);

        List<String> apduData = new ArrayList<>();
        for (CommandApdu chainCommand : chainedApdus) {
            apduData.add(Hex.toHexString(chainCommand.toBytes()));
        }

        return apduData;
    }
}