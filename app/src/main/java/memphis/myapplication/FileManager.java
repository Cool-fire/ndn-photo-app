package memphis.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.util.Blob;

//import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.util.ArrayList;

public class FileManager {

    private String m_appRootPath;
    private File m_friendsDir;
    private File m_selfDir;
    private File m_photosDir;
    private File m_profilePhoto;
    private File m_filesDir;
    private File m_rcvdFilesDir;
    public static boolean dirsCreated = false;

    public FileManager(Context context) {
        /* Eventually, we will set m_appRootPath to getFilesDir(). This is the internal storage of
           the phone. It is inaccessible by other applications. getExternalFilesDir() is on the
           SD card, which allows us to browse these directories with a File Explorer. This is better
           for testing, since you can verify file activity results have occurred. In the Release,
           though, we will not want people to view files outside our app given the Snapchat philosophy
           of destroying content after viewing.
         */
        // m_appRootPath = context.getFilesDir().toString();
        m_appRootPath = context.getExternalFilesDir(null).toString();
        m_friendsDir = new File(m_appRootPath, "/friends");
        m_selfDir = new File(m_appRootPath, "/self");
        m_photosDir = new File(m_appRootPath, "/photos");
        m_filesDir = new File(m_appRootPath, "/files");
        m_rcvdFilesDir = new File(m_appRootPath, "/received_files");

        if(!dirsCreated) {
            createDirs();
        }

        m_profilePhoto = new File(m_photosDir, "profilePhoto.jpg");
    }

    public String getAppRootPath() {
        return m_appRootPath;
    }

    public String getFriendsDir() {
        return m_friendsDir.toString();
    }

    public String getSelfDir() {
        return m_selfDir.toString();
    }

    public String getPhotosDir() {
        return m_photosDir.toString();
    }

    public File getProfilePhoto() {return m_profilePhoto; }

    public String getFilesDir() {
        return m_filesDir.toString();
    }

    public String getRcvdFilesDir() {return m_rcvdFilesDir.toString(); }

    public void setProfilePhoto(Bitmap bitmap) {
        try{
            FileOutputStream stream = new FileOutputStream(m_profilePhoto);
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream);
            stream.flush();
            stream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates the necessary directories for app usage. This includes a friends directory where
     * each of the user's friends' public key and photos will be stored. It also includes a keys
     * directory to store the keypair of the user. And lastly there is a photos directory which
     * stores the user's own photos. This method is intended to be called once upon successful
     * signup for an account.
     *
     * @return Returns a boolean value indicating whether all directories were created or not.
     */
    private void createDirs() {

        boolean madeFriends = (m_friendsDir).mkdir();
        boolean madeSelf = (m_selfDir).mkdir();
        boolean madePhotos = (m_photosDir).mkdir();
        boolean madeFiles = (m_filesDir).mkdir();
        boolean madeRcvdFiles = (m_rcvdFilesDir).mkdir();
        String s = ("" + madeFriends + " " + madeSelf + " " + madePhotos + " " + madeFiles + " " + madeRcvdFiles);
        Log.d("createDirs", s);
        dirsCreated = true;
    }

    /**
     * Saves your username to file so we can retrieve it later.
     * @param username is the chosen name of the user
     * @return whether the file operation was successful or not.
     */
    protected boolean saveUsername(String username) {
        File user = new File(m_selfDir + "/username");
        if (!user.exists()) {
            try {
                user.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileWriter writer = new FileWriter(user);
            writer.write(username);
            writer.close();
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Retrieves your username from file. Needed for namespacing.
     * @return String of username
     */
    public String getUsername() {
        File user = new File(m_selfDir + "/username");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(user));
            String username = reader.readLine();
            reader.close();
            return username;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Saves your personal QR code to a png file.
     * @param myInfo is the bitmap of our QR code we are saving.
     */
    public void saveMyQRCode(Bitmap myInfo) {
        File myQRFile = new File(getMyQRPath());
        try {
            FileOutputStream fostream = new FileOutputStream(myQRFile);
            myInfo.compress(Bitmap.CompressFormat.PNG, 90, fostream);
            fostream.close();
        } catch (FileNotFoundException e) {
            Log.d("saveYourself", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("saveYourself", "Error accessing file: " + e.getMessage());
        }
    }

    /**
     * Gives the path to your personal QR code.
     * @return String path to QR code
     */
    protected String getMyQRPath() {
        return (m_selfDir + "/myQR.png");
    }

    /**
     * Reads the public rsa key file and extracts the public key.
     * @return user's public key in string format
     */
    public net.named_data.jndn.security.certificate.PublicKey getPubKey() {
        try {
            // Certificate
            return Globals.identityManager.getPublicKey(Globals.pubKeyName);
        }
        catch (SecurityException e) {
            e.printStackTrace();
            return null;
        }
    }

    // save friends
    public int saveFriend(String friendContent) {
        if (friendContent.length() > 0) {
            int index = friendContent.indexOf(" ");
            String username = friendContent.substring(0, index);
            // A friend's filename will be their username. Another reason why we must ensure uniqueness
            File friendFile = new File(m_friendsDir + "/" + username);
            if (friendFile.exists()) {
                return 1;
            }
            String pubKey = friendContent.substring(index + 1);
            Log.d("pubKey", "This is what we're writing: " + pubKey);
            try {
                boolean wasCreated = friendFile.createNewFile();
                if(wasCreated) {
                    // consider changing to byte content
                    FileOutputStream fostream = new FileOutputStream(friendFile);
                    // writer.write(username);
                    fostream.write(pubKey.getBytes());
                    fostream.close();
                    return 0;
                }
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    public ArrayList<String> getFriendsList() {
        ArrayList<String> friendsList = new ArrayList<>();
        File[] files = m_friendsDir.listFiles();
        for(File file : files) {
            friendsList.add(file.getName());
        }
        return friendsList;
    }

    /*public Blob getFriendKey(Interest interest) {
        String s = interest.getName().toUri();
        // want the second '/' from /ndn-snapchat/username/... so we can extract username
        int index = s.substring(1).indexOf('/');
        String beginning = s.substring(index+1);
        String username = beginning.substring(0, beginning.indexOf('/'));
        try {
            File friendFile = new File(m_friendsDir + "/" + username);
            if(friendFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(friendFile));
                StringBuffer strBuff = new StringBuffer();
                String line;
                while((line = br.readLine()) != null) {
                    strBuff.append(line);
                }
                Blob blob = new Blob((strBuff.substring(strBuff.indexOf(' '))).getBytes());
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }*/

    public Blob getFriendKey(String friend) {
        try {
            File friendFile = new File(m_friendsDir + "/" + friend);
            if(friendFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(friendFile));
                StringBuffer strBuff = new StringBuffer();
                String line;
                while((line = br.readLine()) != null) {
                    strBuff.append(line);
                }
                // The string we have saved to format is the DER bytes in Base64. We need to revert
                // back to the original format
                byte[] keyBytes = Base64.decode(strBuff.toString(), Base64.DEFAULT);
                Blob key = new Blob(keyBytes);
                Log.d("getFriendKey", key.toString());
                return key;
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Saves data we retrieved from SegmentFetcher to file.
     * @param content The blob of content we received upon Interest.
     * @param path The path of the file we will save it to.
     * @return whether the file operation was successful or not.
     */
    public boolean saveContentToFile(Blob content, String path) {
        String filename = path.substring(path.lastIndexOf("/")+1);
        File dir = new File(m_appRootPath + "/received_files");
        dir.mkdirs();
        File file = new File(m_appRootPath + "/received_files/" + filename);
        if(file.exists()) {
            boolean exists = true;
            int copyNum = 1;
            int fileTypeIndex = filename.lastIndexOf(".");
            while(exists) {
                file = new File(m_appRootPath + "/received_files/" +
                        filename.substring(0, fileTypeIndex) + "(" + copyNum + ")" +
                        filename.substring(fileTypeIndex));
                copyNum++;
                if(!file.exists()) {
                    exists = false;
                }
            }
        }
        byte[] byteContent = content.getImmutableArray();
        try {
            FileOutputStream fostream = new FileOutputStream(file);
            fostream.write(byteContent);
            fostream.close();
            return true;
        }
        catch(FileNotFoundException e) {
            e.printStackTrace();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Saves the Name of your file in a QR code. You can present this code to a friend to scan for
     * easy retrieval.
     * @param bitmap provided bitmap to form png of QR code
     * @param path provided filepath; we will use this to name the file after changing its type to png
     * @return true or false depending on success
     */
    public boolean saveFileQR(Bitmap bitmap, String path) {
        // do file operations here to remove .txt or .pdf or whatever and append .png
        int dotIndex = path.lastIndexOf(".");
        String filename = path.substring(path.lastIndexOf("/")+1, dotIndex) + ".png";
        File fileQR = new File(m_filesDir + "/" + filename);
        if (!fileQR.exists()) {
            try {
                fileQR.createNewFile();
                FileOutputStream fostream = new FileOutputStream(fileQR);
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fostream);
                fostream.close();
                return true;
            } catch (FileNotFoundException e) {
                Log.d("saveFileQR", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("saveFileQR", "Error accessing file: " + e.getMessage());
            }
            return false;
        }
        return false;
    }

    // need to map files to one another; start off with full path where we append /ndn-snapchat/<username>
    // or remove /ndn-snapchat/<username>, depending on which way we are going. Then, incorporate a hash.

    // ok; this shows us the full path now. That was easy. Now let's do some hashing for security purposes.
    public String findFile(String providedPath) {
        // remove /ndn-snapchat/<username>, gives /path/to/file
        String trimmed = providedPath;
        int index = 0;
        try {
            for (int i = 0; i < 2; i++) {
                // we already know "/" is at index 0, but we need to find the 3rd slash
                index = trimmed.indexOf("/", index + 1);
            }
        }
        catch(IndexOutOfBoundsException e) {
            e.printStackTrace();
            Log.d("findFile", "IndexOutOfBoundsException. The provided path does not follow file naming convention.");
        }

        trimmed = trimmed.substring(index);
        return trimmed;

        // de-hash trimmed
        // return de-hashed trimmed

        // or we could serve from the same directory every time, so any file we are serving has either
        // a symbolic or direct link to the actual file location; how about we add a link to the
        // full path each time we need to publish the data?

        // maybe we hash and then we make a symbolic link based on the hash; this way we obscure
        // the path location, but there are no collisions in the common directory

    }

    /**
     * prepends "/ndn-snapchat/<username>" to file path; temporarily static for testing purposes
     * @param path the provided absolute path of the file
     * @return string of the form /ndn-snapchat/<username>/path/to/file
     */
    public String addAppPrefix(String path) {
        // we could also allow the user to state their own name which will attach to the end of
        // /ndn-snapchat/<username>/
        String username = this.getUsername();
        if (username != null) {
            // check that path already comes with "/" prepended
            if(path.charAt(0) == '/') {
                return "/ndn-snapchat/" + username + path;
            }
            else {
                return "/ndn-snapchat/" + username + "/" + path;
            }
        }
        else {
            return null;
        }
    }

    /**
     * removes the prefix "/ndn-snapchat/<username>" so we can find the file
     * @param fullname : "ndn-snapchat/<username>/path/to/file"
     * @return String of file path
     */
    public static String removeAppPrefix(String fullname) {
        int fileIndex = 0;
        String temp = fullname.substring(fileIndex);
        // name is of the form /ndn-snapchat/username/full-file-path; find third instance of "/"
        for(int i = 0; i < 3; i++) {
            fileIndex = temp.indexOf("/");
            temp = temp.substring(fileIndex + 1);
        }
        return temp;
    }

    // we need to hash to some common directory or we will need to have a table that contain links to the files
}
