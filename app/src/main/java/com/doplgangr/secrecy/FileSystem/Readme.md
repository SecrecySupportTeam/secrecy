FileSystem of Secrecy

Here you will find a few classes.

Cryptology:
AES_Encryptor:
Class to handle encryption, returning CipherStreams

OurFileProvider:
File Provider that exposes the encrypted files to the viewing apps. The only outlet of decrypted files.

FileObserver:
Monitors temp folder, killing anything that gets dumped to the temp folder after some Grace Period.

(Grace period:
This is one of the difference of Secrecy that makes it safe. Instead of creating a persistent tempfile for viewing and killing it after not needed, it kills the file RIGHT AFTER it has been open. This is based on the fact that the FILE object is just a reference to a certain position on the disk. Also, in the android environment any deletion action is just marking a file as deleted, the actual content is retained. Any FileInputStream opened by the apps still can access the files.
Thus, we pass the reference of file to the opening app, wait for them to open the file, wait past grace period, assume that they already have a InputStream or something, and delete the temp file.
This approach is better than waiting for the close action since we are not sure that the apps will close the files gracefully after they are done with it.)

Other classes:
File:
Representation of an encrypted file
Vault:
Representation of a folder containing encrypted files, thumbnails and a .nomedia file that serves as simple key tester.
FileOptionsService:
Service to do long spanning jobs, like adding files and deleting temps

