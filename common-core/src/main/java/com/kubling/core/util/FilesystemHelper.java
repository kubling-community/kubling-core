/*
 * This file was modified as part of the Kubling project.
 */

package com.kubling.core.util;

import org.apache.commons.lang3.Strings;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.util.RandomAccessMode;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class FilesystemHelper {

    public static final long TRANSFER_SIZE = 8192;
    private static FileSystemManager manager;

    public static synchronized FileSystemManager getManager() throws FileSystemException {

        if (Objects.nonNull(manager)) return manager;

        var localManager = new StandardFileSystemManager();
        localManager.setConfiguration(Thread.currentThread()
                .getContextClassLoader().getResource("com/kubling/core/providers.xml"));
        localManager.init();

        manager = localManager;

        return manager;
    }

    public static FileChannel newFileChannel(URI uri, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
            throws IOException {

        // This method can be called directly, therefore we need to ensure that manager was already created
        if (Objects.isNull(manager)) manager = getManager();

        var fileObject = manager.resolveFile(uri);
        if (options.contains(StandardOpenOption.CREATE_NEW) && fileObject.exists())
            throw new FileAlreadyExistsException(uri.toString());
        else if (!fileObject.exists()
                && (options.contains(StandardOpenOption.CREATE_NEW) || options.contains(StandardOpenOption.CREATE)))
            fileObject.createFile();
        var content = fileObject.getContent();
        var rac = content.getRandomAccessContent(toRandomAccessMode(options));
        return new FileChannel() {

            @Override
            public int read(ByteBuffer dst) throws IOException {
                var arr = new byte[dst.remaining()];
                int r = rac.getInputStream().read(arr, 0, arr.length);
                if (r > 0)
                    dst.put(arr, 0, r);
                return r;
            }

            @Override
            public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
                long t = 0;
                for (var dst : dsts) {
                    var arr = new byte[dst.remaining()];
                    int r = rac.getInputStream().read(arr, offset, length);
                    if (r > 0)
                        dst.put(arr, 0, r);
                    t += r;
                }
                return t;
            }

            @Override
            public int write(ByteBuffer src) throws IOException {
                var arr = new byte[src.remaining()];
                src.get(arr);
                rac.write(arr);
                return arr.length;
            }

            @Override
            public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
                long t = 0;
                for (var src : srcs) {
                    var arr = new byte[src.remaining()];
                    src.get(arr);
                    rac.write(arr, offset, length);
                    t += arr.length;
                }
                return t;
            }

            @Override
            public long position() throws IOException {
                return rac.getFilePointer();
            }

            @Override
            public FileChannel position(long newPosition) throws IOException {
                rac.seek(newPosition);
                return this;
            }

            @Override
            public long size() throws IOException {
                return rac.length();
            }

            @Override
            public FileChannel truncate(long size) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void force(boolean metaData) {
                // Noop?
            }

            @Override
            public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
                // Untrusted target: Use a newly-erased buffer
                int c = (int) Math.min(count, TRANSFER_SIZE);
                ByteBuffer bb = ByteBuffer.allocate(c);
                long tw = 0; // Total bytes written
                long pos = position;
                try {
                    while (tw < count) {
                        bb.limit((int) Math.min(count - tw, TRANSFER_SIZE));
                        int nr = read(bb, pos);
                        if (nr <= 0)
                            break;
                        bb.flip();
                        // ## Bug: Will block writing target if this channel
                        // ## is asynchronously closed
                        int nw = target.write(bb);
                        tw += nw;
                        if (nw != nr)
                            break;
                        pos += nw;
                        bb.clear();
                    }
                    return tw;
                } catch (IOException x) {
                    if (tw > 0)
                        return tw;
                    throw x;
                }
            }

            @Override
            public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
                // Untrusted target: Use a newly-erased buffer
                int c = (int) Math.min(count, TRANSFER_SIZE);
                ByteBuffer bb = ByteBuffer.allocate(c);
                long tw = 0; // Total bytes written
                long pos = position;
                try {
                    while (tw < count) {
                        bb.limit((int) Math.min((count - tw), TRANSFER_SIZE));
                        // ## Bug: Will block reading src if this channel
                        // ## is asynchronously closed
                        int nr = src.read(bb);
                        if (nr <= 0)
                            break;
                        bb.flip();
                        int nw = write(bb, pos);
                        tw += nw;
                        if (nw != nr)
                            break;
                        pos += nw;
                        bb.clear();
                    }
                    return tw;
                } catch (IOException x) {
                    if (tw > 0)
                        return tw;
                    throw x;
                }
            }

            @Override
            public int read(ByteBuffer dst, long position) throws IOException {
                position(position);
                return read(dst);
            }

            @Override
            public int write(ByteBuffer src, long position) throws IOException {
                position(position);
                return write(src);
            }

            @Override
            public MappedByteBuffer map(MapMode mode, long position, long size) {
                throw new UnsupportedOperationException();
            }

            @Override
            public FileLock lock(long position, long size, boolean shared) {
                throw new UnsupportedOperationException();
            }

            @Override
            public FileLock tryLock(long position, long size, boolean shared) {
                throw new UnsupportedOperationException();
            }

            @Override
            protected void implCloseChannel() throws IOException {
                rac.close();
            }

        };
    }

    public static RandomAccessMode toRandomAccessMode(Set<? extends OpenOption> options) {
        if (options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.CREATE)
                || options.contains(StandardOpenOption.CREATE_NEW))
            return RandomAccessMode.READWRITE;
        return RandomAccessMode.READ;
    }

    public static byte[] readFileContentFromZip(String pathToFile, FileObject zipFile)
            throws IOException {

        pathToFile = Strings.CS.removeStart(pathToFile, "/");
        String[] children = pathToFile.split("/");
        FileObject pathFo = zipFile;
        FileObject contentFo = null;
        int i = 0;
        for (String c : children) {

            if (Objects.isNull(pathFo)) throw new IOException(
                    String.format("File <<%s>> does not exist in <<%s>>.", pathToFile, zipFile.getName().getFriendlyURI()));

            i++;
            if (i == children.length) {
                contentFo = pathFo.getChild(c);
                if (Objects.isNull(contentFo) || !contentFo.exists()) {
                    throw new IOException(
                            String.format("File <<%s>> does not exist in <<%s>>.", c, zipFile.getName().getFriendlyURI()));
                }
            } else {
                pathFo = pathFo.getChild(c);
            }

        }

        return contentFo.getContent().getByteArray();

    }

    public static List<String> listFileNameOfDirFromZip(String pathToDir, FileObject zipFile)
            throws IOException {

        pathToDir = Strings.CS.removeStart(pathToDir, "/");
        String[] children = pathToDir.split("/");
        FileObject pathFo = zipFile;

        int i = 0;
        for (String c : children) {

            if (Objects.isNull(pathFo)) throw new IOException(
                    String.format("Directory <<%s>> does not exist in <<%s>>.",
                            pathToDir, zipFile.getName().getFriendlyURI()));

            i++;
            if (i == children.length) {
                pathFo = pathFo.getChild(c);
                if (Objects.isNull(pathFo) || !pathFo.exists()) {
                    throw new IOException(
                            String.format("Directory <<%s>> does not exist in <<%s>>.",
                                    c, zipFile.getName().getFriendlyURI()));
                }
            } else {
                pathFo = pathFo.getChild(c);
            }

        }

        return Arrays.stream(pathFo.getChildren()).map(fo -> fo.getName().getBaseName()).collect(Collectors.toList());

    }
}
