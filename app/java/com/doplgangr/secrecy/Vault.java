package com.doplgangr.secrecy;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.CipherOutputStream;

/**
 * Created by Matthew on 3/22/2014.
 */
public class Vault {
    String name;
    String path;
    ArrayList<File> files = new ArrayList<File>();
    Boolean wrongPass = true;
    private String key;

    public Vault(String name, String secret) {
        this.key = secret;
        this.name = name;
        path = storage.ROOT() + "/" + name;
        initialize();
    }

    public Vault(String name, String secret, Boolean temp) {
        this.key = secret;
        this.name = name;
        path = storage.ROOT() + "/" + name;
    }

    public void initialize() {
        storage.get().createDirectory(path);
        String regex = "^((?!_thumb|.nomedia).)*$";
        List<java.io.File> absFiles = storage.get().getFiles(path, regex);
        java.io.File nomedia = new java.io.File(storage.getRoot().getAbsolutePath() + "/" +
                name + "/.nomedia");
        if (!nomedia.exists())
            return;
        File nomediafile = new File(nomedia, key);
        java.io.File tempnomedia = nomediafile.readFile(new CryptStateListener() {
            @Override
            public void updateProgress(int progress) {
            }

            @Override
            public void setMax(int max) {
            }

            @Override
            public void onFailed(int statCode) {
            }

            @Override
            public void Finished() {

            }
        });
        if (tempnomedia != null) {
            wrongPass = false;
            tempnomedia.delete();
        }
        Log.d("Password is Wrong=", wrongPass + "");

        files.clear();
        for (java.io.File absfile : absFiles)
            files.add(new File(absfile, key));
    }

    public String addFile(final Context context, final Uri uri) {
        String filename = uri.getLastPathSegment();
        if (!filename.contains("_thumb")) {
            ContentResolver cR = context.getContentResolver();
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            String type = mime.getExtensionFromMimeType(cR.getType(uri));
            if (type != null)
                filename = FilenameUtils.removeExtension(filename) + "." + type;
        }
        InputStream is = null;
        OutputStream out = null;
        try {
            InputStream stream = context.getContentResolver().openInputStream(uri);
            storage.get().deleteFile(path, filename);
            storage.get().
                    getFile(path, filename)
                    .createNewFile();
            is = new BufferedInputStream(stream);
            byte buffer[] = new byte[Config.bufferSize];
            int count;
            AESEnc enc = new AESEnc(key);
            out = new CipherOutputStream(new FileOutputStream(storage.get().
                    getFile(path, filename)),
                    enc.encryptstream()
            );
            while ((count = is.read(buffer)) != -1)
                out.write(buffer, 0, count);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
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
        initialize();
        return filename;
    }

    public Boolean delete() {
        if (!wrongPass)
            storage.get().deleteDirectory(path);
        return !wrongPass;
    }

}
