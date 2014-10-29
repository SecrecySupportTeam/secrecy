/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.doplgangr.secrecy.FileSystem.Files;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.FileSystem.CryptStateListener;
import com.doplgangr.secrecy.FileSystem.Encryption.Crypter;
import com.doplgangr.secrecy.FileSystem.Storage;
import com.doplgangr.secrecy.Util;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.crypto.CipherInputStream;

public class SecrecyFile implements Serializable {

    protected String decryptedFileName;
    protected String fileSize;
    protected Date timestamp;

    public String getFileExtension() {
        return fileExtension;
    }

    protected String fileExtension;
    protected File file;

    protected Boolean isDecrypting = false;
    protected Crypter crypter;

    protected static String humanReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        char pre = ("KMGTPE").charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public String getDecryptedFileName() {
        return decryptedFileName;
    }

    public String getFileSize() {
        return fileSize;
    }

    public String getType() {
        return fileExtension;
    }

    public File getFile() {
        return file;
    }

    public String getTimestamp() {
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        if (timestamp == null)
            return "";
        return df.format(timestamp);
    }

    public Boolean getIsDecrypting() {
        return isDecrypting;
    }

    public File readFile(CryptStateListener listener) {
        isDecrypting = true;
        InputStream is = null;
        OutputStream out = null;
        File outputFile = null;
        try {
            outputFile = File.createTempFile("tmp" + decryptedFileName, "." +
                    fileExtension, Storage.getTempFolder());
            outputFile.mkdirs();
            outputFile.createNewFile();
            is = crypter.getCipherInputStream(getFile());
            listener.setMax((int) file.length());
            ReadableByteChannel inChannel = Channels.newChannel(is);
            FileChannel outChannel = new FileOutputStream(outputFile).getChannel();
            ByteBuffer byteBuffer = ByteBuffer.allocate(Config.bufferSize);
            while (inChannel.read(byteBuffer) >= 0 || byteBuffer.position() > 0) {
                byteBuffer.flip();
                outChannel.write(byteBuffer);
                byteBuffer.compact();
                listener.updateProgress((int) outChannel.size());
            }
            inChannel.close();
            outChannel.close();
            Util.log(outputFile.getName(), outputFile.length());
            return outputFile;
        } catch (FileNotFoundException e) {
            listener.onFailed(2);
            Util.log("Encrypted File is missing", e.getMessage());
        } catch (IOException e) {
            Util.log("IO Exception while decrypting", e.getMessage());
            if (e.getMessage().contains("pad block corrupted"))
                listener.onFailed(1);
            else
                e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            listener.Finished();
            isDecrypting = false;
            try {
                if (is != null) {
                    is.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // An error occured. Too Bad
        if (outputFile != null)
            Storage.purgeFile(outputFile);
        return null;
    }

    public CipherInputStream readStream(CryptStateListener listener) {
        try {
            return crypter.getCipherInputStream(getFile());
        } catch (Exception e) {
        }
        return null;
    }

    public void delete() {
        Storage.purgeFile(file);
    }
}
