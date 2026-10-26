package com.gathin.tvhome;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Android's android.util.AtomicFile without the API 17 requirement. Same contract: writes go to
 * {@code <base>.new} and replace the file only on {@link #finishWrite}; a failed write restores the previous
 * contents. Used for the launcher's small preference files, so a TV losing power mid-write never leaves half
 * a file behind.
 */
final class AtomicFile {

    private final File base;
    private File backup;
    private File newFile;

    AtomicFile(File base) {
        this.base = base;
    }

    File getBaseFile() {
        return base;
    }

    FileOutputStream startWrite() throws IOException {
        if (newFile == null) newFile = new File(base.getPath() + ".new");
        if (backup == null) backup = new File(base.getPath() + ".bak");

        // Keep the old contents aside until the new file is complete.
        if (base.exists()) {
            if (backup.exists()) backup.delete();
            if (!base.renameTo(backup)) {
                backup.delete();
                throw new IOException("cannot back up " + base);
            }
        }
        // A .new left by a crash is stale.
        if (newFile.exists() && !newFile.delete()) throw new IOException("cannot clear " + newFile);
        File parent = base.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("cannot create " + parent);
        return new FileOutputStream(newFile);
    }

    void finishWrite(FileOutputStream stream) {
        if (stream == null) return;
        try {
            stream.getFD().sync();
        } catch (IOException ignored) {
            // Some file systems do not support fsync; the rename below is still atomic.
        }
        try {
            stream.close();
        } catch (IOException ignored) {
        }
        if (base.exists()) base.delete();
        if (!newFile.renameTo(base)) {
            // Leave .new in place; it is still readable by openRead through the crash path.
            return;
        }
        if (backup != null && backup.exists()) backup.delete();
    }

    void failWrite(FileOutputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }
        if (newFile != null && newFile.exists()) newFile.delete();
        if (base.exists()) return;
        if (backup != null && backup.exists()) backup.renameTo(base);
    }

    FileInputStream openRead() throws FileNotFoundException {
        if (base.exists()) return new FileInputStream(base);
        // A write that was interrupted before the rename left the previous contents here.
        if (backup != null && backup.exists()) return new FileInputStream(backup);
        if (newFile == null) newFile = new File(base.getPath() + ".new");
        if (newFile.exists()) return new FileInputStream(newFile);
        throw new FileNotFoundException(base.getPath());
    }

    byte[] readFully() throws IOException {
        try (FileInputStream input = openRead()) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            return output.toByteArray();
        }
    }
}
