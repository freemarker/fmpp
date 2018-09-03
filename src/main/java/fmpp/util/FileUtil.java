/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fmpp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;

/**
 * Collection of file and path related functions.
 */
public class FileUtil {
    /**
     * Reaturns the path of a file or directory relative to a directory,
     * in native format.
     * @return The relative path.
     *     It never starts with separator char (/ on UN*X).
     * @throws IOException if the two paths has no common parent directory
     *   (such as <code>C:\foo.txt</code> and <code>D:\foo.txt</code>), or
     *   the the paths are malformed.
     */
    public static String getRelativePath(File fromDir, File toFileOrDir)
            throws IOException {
        char sep = File.separatorChar;
        String ofrom = fromDir.getCanonicalPath();
        String oto = toFileOrDir.getCanonicalPath();
        boolean needSepEndForDirs;
        if (!ofrom.endsWith(File.separator)) {
            ofrom += sep;
            needSepEndForDirs = false;
        } else {
            needSepEndForDirs = true;
        }
        boolean otoEndsWithSep;
        if (!oto.endsWith(File.separator)) {
            oto += sep;
            otoEndsWithSep = false;
        } else {
            otoEndsWithSep = true;
        }
        String from = ofrom.toLowerCase();
        String to = oto.toLowerCase();
        
        StringBuffer path = new StringBuffer(oto.length());

        int fromln = from.length();
        goback: while (true) {
            if (to.regionMatches(0, from, 0, fromln)) {
                File fromf = new File(ofrom.substring(
                        0, needSepEndForDirs ? fromln : fromln - 1));
                File tof = new File(oto.substring(
                        0, needSepEndForDirs ? fromln : fromln - 1));
                if (fromf.equals(tof)) {
                    break goback;
                }
            }
            path.append(".." + sep);
            fromln--;
            while (fromln > 0 && from.charAt(fromln - 1) != sep) {
                fromln--;
            }
            if (fromln == 0) {
                throw new IOException(
                        "Could not find common parent directory in these "
                        + "paths: " + ofrom + " and " + oto);
            }
        }
        path.append(oto.substring(fromln));
        if (!otoEndsWithSep && path.length() != 0) {
            path.setLength(path.length() - 1);
        }

        return path.toString();
    }
    
    /**
     * Copies a file; silently overwrites the destination if already exists.
     * 
     * @param copyLMD tells if the last modification time of the original
     *     file will be copied too.
     */
    public static void copyFile(File src, File dst, boolean copyLMD)
            throws IOException {
        byte[] buffer = new byte[1024 * 64];
        InputStream in = new FileInputStream(src);
        try {
            long srcLMD = 0L;
            if (copyLMD) {
                srcLMD = src.lastModified();
                if (srcLMD == 0) {
                    throw new IOException("Failed to get the last modification "
                            + "time of " + src.getAbsolutePath());
                }
            }
            OutputStream out = new FileOutputStream(dst);
            try {
                int ln;
                while ((ln = in.read(buffer)) != -1) {
                    out.write(buffer, 0, ln);
                }
            } finally {
                out.close();
            }
            if (srcLMD != 0L) {
                dst.setLastModified(srcLMD);
            }
        } finally {
            in.close();
        }
    }

    /**
     * Same as {@link #copyFile(File, File, boolean) copyFile(src, dst, true))}.
     */
    public static void copyFile(File src, File dst) throws IOException {
        copyFile(src, dst, true);
    }
    
    /**
     * Returns true if <code>file</code> is inside <code>ascendant</code> or
     * <code>file</code> is the same as the <code>ascendant</code>, otherwise
     * returns false.
     */
    public static boolean isInsideOrEquals(File file, File ascendant) {
        while (!file.equals(ascendant)) {
            file = file.getParentFile();
            if (file == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if <code>file</code> is inside <code>ascendant</code>,
     * otherwise returns false.
     */
    public static boolean isInside(File file, File ascendant) {
        if (!file.equals(ascendant)) {
            return false;
        }
        do {
            file = file.getParentFile();
            if (file == null) {
                return false;
            }
        } while (!file.equals(ascendant));
        return true;
    }
    
    /**
     * Resolves relative UN*X path based on given root and working directory.
     * @param root root directory
     * @param wd working directory (current direcory)
     */
    public static File resolveRelativeUnixPath(File root, File wd, String path)
            throws IOException {
        File c;
        int i;
        if (path.startsWith("/")) {
            i = 1;
            c = root.getAbsoluteFile();
        } else {
            i = 0;
            c = wd.getAbsoluteFile();
        }
        int ln = path.length();
        while (i < ln) {
            String step;
            int si = path.indexOf(i, '/');
            if (si == -1) {
                si = ln;
            }
            step = path.substring(i, si);
            i = si + 1;
            if (step.equals(".")) {
                continue;
            } else if (step.equals("..")) {
                c = c.getParentFile();
                if (c == null) {
                    throw new IOException("Parent directory not found.");
                }
            } else {
                c = new File(c, step);
            }
        }
        c = c.getCanonicalFile();
        
        if (!isInsideOrEquals(c, root)) {
            throw new IOException("Attempt to leave the root directory.");
        }
        
        return c;
    }
    
    /**
     * Returns a compressed version of the path.
     * For example, <code>/foo/ba.../baaz.txt</code> instead of
     * <code>/foo/bar/blah/blah/blah/baaz.txt</code>.
     * @param path the path to compress. Either native or UNIX format.
     * @param maxPathLength the maximum length of the result.
     *     Must be at least 4.
     */
    public static String compressPath(String path, int maxPathLength) {
        if (path.length() > maxPathLength) {
            int r = path.length() - maxPathLength + 3;
            int i = path.lastIndexOf(File.separatorChar);
            if (i == -1) {
                i = path.lastIndexOf('/');
            }
            if (i <= r) {
                return "..." + path.substring(r);
            } else {
                return path.substring(0, i - r) + "..." + path.substring(i);
            }
        } else {
            return path;
        }
    }

    /**
     * Brings the path to UNI*X style format, so that it can be handled
     * with path pattern handling functions.
     */
    public static String pathToUnixStyle(String path) {
        path = path.replace(File.separatorChar, '/');
        if (File.separatorChar != '\\') {
            path = path.replace('\\', '/'); // Protection from Win users... :)
        }
        return path;
    }

    public static String removeSlashPrefix(String path) {
        if (path.startsWith("/") && !path.startsWith("//")) {
            return path.substring(1);
        } else {
            return path;
        }
    }

    /**
     * Returns the part of the name after the last dot, or if there's no dot, {@code null}.
     * @param name The file name or path. If {@code null}, {@code null} is returned.
     * @since 0.9.16
     */
    public static String getFileExtension(String name) {
        if (name == null) {
            return null;
        }
        
        int dotIdx = name.lastIndexOf('.');
        return dotIdx != -1 ? name.substring(dotIdx + 1) : null; 
    }
    
    /**
     * Same as {@link #getFileExtension(String)}, but also converts the result to lower case. 
     * @since 0.9.16
     */
    public static String getLowerCaseFileExtension(String name) {
        String ext = getFileExtension(name);
        return ext != null ? ext.toLowerCase() : null;
    }

    /**
     * Converts UN*X style path to regular expression (originally, for Perl 5 dialect, but also works for Java's
     * dialect).
     * In additional to standard UN*X path meta characters (<code>*</code>,
     * <code>?</code>) it understands <code>**</code>, that is the same as
     * in Ant. It assumes that the paths what you will later match
     * with the pattern are always starting with slash (they are absolute paths
     * to an imaginary base).
     */
    public static String pathPatternToPerl5Regex(String text) {
        StringBuffer sb = new StringBuffer();
    
        if (!text.startsWith("/")) {
            text = "/" + text;
        }
        if (text.endsWith("/")) {
            text += "**";
        }
        
        char[] chars = text.toCharArray();
        int ln = chars.length;
        for (int i = 0; i < ln; i++) {
            char c = chars[i];
            if (c == '\\' || c == '^' || c == '.' || c == '$' || c == '|'
                    || c == '(' || c == ')' || c == '[' || c == ']'
                    || c == '+' || c == '{'
                    || c == '}' || c == '@') {
                sb.append('\\');
                sb.append(c);
            } else if (i == 0 && ln > 2
                    && chars[0] == '*' && chars[1] == '*'
                    && chars[2] == '/') {
                sb.append(".*/");
                i += 2;
            } else if (c == '/' && i + 2 < ln
                    && chars[i + 1] == '*' && chars[i + 2] == '*') {
                if (i + 3 == ln) {
                    sb.append("/.*");
                } else {
                    sb.append("(/.*)?");
                }
                i += 2;
            } else if (c == '*') {
                sb.append("[^/]*");
            } else if (c == '?') {
                sb.append("[^/]");
            } else {
                sb.append(c);
            }
        }
    
        return sb.toString();
    }
    
    public static String loadString(InputStream in, String charset)
            throws IOException {
        Reader r = charset != null ? new InputStreamReader(in, charset) : new InputStreamReader(in);
        StringBuffer sb = new StringBuffer(1024);
        try {
            char[] buf = new char[4096];
            int ln;
            while ((ln = r.read(buf)) != -1) {
                sb.append(buf, 0, ln);
            }
        } finally {
            r.close();
        }
        return sb.toString();
     }
     
    public static byte[] loadByteArray(InputStream in) throws IOException {
        int size = 0;
        int bcap = 1024;
        byte[] b = new byte[bcap];
        try {
            int rdn;
            while ((rdn = in.read(b, size, bcap - size)) != -1) {
                size += rdn;
                if (bcap == size) {
                    bcap *= 2;
                    byte[] newB = new byte[bcap];
                    System.arraycopy(b, 0, newB, 0, size);
                    b = newB;
                }
            }
        } finally {
            in.close();
        }
        if (b.length != size) {
            byte[] newB = new byte[size];
            System.arraycopy(b, 0, newB, 0, size);
            return newB;
        } else {
            return b;
        }
    }
    
}