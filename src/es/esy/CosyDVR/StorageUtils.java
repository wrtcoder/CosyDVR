/*Got from http://stackoverflow.com/questions/5694933/find-an-external-sd-card-location*/
package es.esy.CosyDVR;

import android.content.Context;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.StringTokenizer;
import android.os.Build;
import android.os.Environment;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import android.util.Log;


public class StorageUtils {

    public class StorageInfo {

        public final String path;
        public final boolean internal;
        public final boolean readonly;
        public final int display_number;

        StorageInfo(String path, boolean internal, boolean readonly, int display_number) {
            this.path = path;
            this.internal = internal;
            this.readonly = readonly;
            this.display_number = display_number;
        }

        public String getDisplayName() {
            StringBuilder res = new StringBuilder();
            if (internal) {
                res.append("Internal " + " [" + path + "]");
            } else {
                res.append("SD card " + display_number + " [" + path + "]");
            }
            if (readonly) {
                res.append(" (Read only)");
            }
            return res.toString();
        }
    }

    public List<StorageInfo> getStorageList(Context context) {
        String BASE_DIR = "/CosyDVR";
        List<StorageInfo> list = new ArrayList<StorageInfo>();
        String def_path = Environment.getExternalStorageDirectory().getPath() + BASE_DIR;
        boolean def_path_internal = !Environment.isExternalStorageRemovable();
        String def_path_state = Environment.getExternalStorageState();
        boolean def_path_available = def_path_state.equals(Environment.MEDIA_MOUNTED)
                                    || def_path_state.equals(Environment.MEDIA_MOUNTED_READ_ONLY);
        boolean def_path_readonly = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY);
        BufferedReader buf_reader = null;
        try {
            HashSet<String> paths = new HashSet<String>();
            buf_reader = new BufferedReader(new FileReader("/proc/mounts"));
            String line;
            int cur_display_number = 1;
            while ((line = buf_reader.readLine()) != null) {
                if (line.contains("vfat") || line.contains("/mnt")) {
                    StringTokenizer tokens = new StringTokenizer(line, " ");
                    String unused = tokens.nextToken(); //device
                    String mount_point = tokens.nextToken() + BASE_DIR; //mount point
                    if (paths.contains(mount_point)) continue;
                    unused = tokens.nextToken(); //file system
                    List<String> flags = Arrays.asList(tokens.nextToken().split(",")); //flags
                    boolean readonly = flags.contains("ro");
                    if (readonly) continue;
                    if (mount_point.equals(def_path)) {
                        paths.add(def_path);
                        list.add(0, new StorageInfo(def_path, def_path_internal, readonly, 0));
                    } else if (line.contains("/dev/block/vold")) {
                        /*if (!line.contains("/mnt/secure")
                            && !line.contains("/mnt/asec")
                            && !line.contains("/mnt/obb")
                            && !line.contains("/dev/mapper")
                            && !line.contains("tmpfs")) {*/
                            paths.add(mount_point);
                            list.add(new StorageInfo(mount_point, false, readonly, cur_display_number++));
                        //}
                    }
                }
            }

            if (!paths.contains(def_path) && def_path_available) {
                list.add(0, new StorageInfo(def_path, def_path_internal, def_path_readonly, -1));
            }
            if (Build.VERSION.SDK_INT >= 21) {
                File[] mediadirs = context.getExternalMediaDirs();
                if (mediadirs != null) {
                    for (int i = 0; i < mediadirs.length; i++) {
                        if (mediadirs[i] == null) continue;
                        list.add(0, new StorageInfo(mediadirs[i].getAbsolutePath(), 
                                !Environment.isExternalStorageRemovable(mediadirs[i]), 
                                Environment.getExternalStorageState(mediadirs[i]) == Environment.MEDIA_MOUNTED_READ_ONLY , -1));
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= 19) {
                File[] filesdirs = context.getExternalFilesDirs("");
                if (filesdirs != null) {
                    for (int i = 0; i < filesdirs.length; i++) {
                        if (filesdirs[i] == null) continue;
                        list.add(0, new StorageInfo(filesdirs[i].getAbsolutePath(), 
                                !Environment.isExternalStorageRemovable(filesdirs[i]), 
                                Environment.getExternalStorageState(filesdirs[i]) == Environment.MEDIA_MOUNTED_READ_ONLY, -1));
                    }
                }
            }

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (buf_reader != null) {
                try {
                    buf_reader.close();
                } catch (IOException ex) {}
            }
        }
        return list;
    }    
}