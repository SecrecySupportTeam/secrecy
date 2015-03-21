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

package com.doplgangr.secrecy.filesystem.files;

import android.widget.ProgressBar;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.exceptions.SecrecyCipherStreamException;
import com.doplgangr.secrecy.filesystem.CryptStateListener;
import com.doplgangr.secrecy.filesystem.encryption.Crypter;
import com.doplgangr.secrecy.filesystem.encryption.SecrecyCipherInputStream;
import com.doplgangr.secrecy.filesystem.Storage;
import com.doplgangr.secrecy.utils.Util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class SecrecyFile implements Serializable {

    String decryptedFileName;
    String fileSize;
    Date timestamp;
    String fileExtension;
    File file;
    Crypter crypter;
    private Boolean isDecrypting = false;
    private ProgressBar progressBar;

    static String humanReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        char pre = ("KMGTPE").charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public void setProgressBar(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    public String getFileExtension() {
        return fileExtension;
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

    public Date getDate() {
        return timestamp;
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

    public void setIsDecrypting(Boolean isDecrypting) {
        this.isDecrypting = isDecrypting;
    }

    public File readFile(CryptStateListener listener) {
        isDecrypting = true;
        InputStream is = null;
        File outputFile = null;
        BufferedOutputStream out = null;
        try {
            outputFile = new File(Storage.getTempFolder() + "/" + decryptedFileName);
            out = new BufferedOutputStream(new FileOutputStream(outputFile), Config.BLOCK_SIZE);
            is = crypter.getCipherInputStream(getFile());
            listener.setMax((int) file.length());

            int readBytes;
            int readTotal = 0;
            byte[] buf = new byte[Config.BUFFER_SIZE];
            while ((readBytes = is.read(buf)) > 0) {
                out.write(buf, 0, readBytes);
                readTotal += readBytes;
                listener.updateProgress(readTotal);
            }
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

    public void rename(String newName) throws SecrecyCipherStreamException, FileNotFoundException {
        crypter.renameFile(getFile(), newName);
        decryptedFileName = newName;
    }

    public SecrecyCipherInputStream readStream() {
        try {
            return crypter.getCipherInputStream(getFile());
        } catch (Exception e) {
            Util.log("Error reading stream!");
        }
        return null;
    }

    public abstract void delete();
}
