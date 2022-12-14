package com.sad.jetpack.v1.utils


import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

public class Compressor {

    //private static Log log = LogFactory.getLog(Compressor.class);

    private static final int BUFFER = 8192;

    private File fileName;

    private String originalUrl;

    public Compressor(String pathName) {
        fileName = new File(pathName);
    }

    public void compress(String... pathName) {
        ZipOutputStream out = null;
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
            CheckedOutputStream cos = new CheckedOutputStream(fileOutputStream,
                    new CRC32());
            out = new ZipOutputStream(cos);
            String basedir = "";
            for (int i = 0; i < pathName.length; i++) {
                compress(new File(pathName[i]), out, basedir);
            }
            out.close();
        } catch (Exception e) {
            if (out!=null){
                out.close()
            }
            throw new RuntimeException(e);
        }
    }

    public void compress(String srcPathName) {
        File file = new File(srcPathName);
        if (!file.exists())
            throw new RuntimeException(srcPathName + "不存在！");
        ZipOutputStream out=null
        FileOutputStream fileOutputStream=null
        CheckedOutputStream cos=null
        try {
            File[] sourceFiles = file.listFiles();
            if(null == sourceFiles || sourceFiles.length<1){
                System.out.println(">*待压缩的文件目录：" + srcPathName + "里面不存在文件，无需压缩.");
            }else{
                fileOutputStream = new FileOutputStream(fileName);
                cos = new CheckedOutputStream(fileOutputStream,
                        new CRC32());
                out = new ZipOutputStream(cos)
                String basedir = ""
                for(int i=0;i<sourceFiles.length;i++){
                    compress(sourceFiles[i], out, basedir)
                }
                out.close()
                fileOutputStream.close()
                cos.close()
            }

        } catch (Exception e) {
            if (out!=null){
                out.close()
            }
            if (fileOutputStream!=null){
                fileOutputStream.close()
            }
            if (cos!=null){
                cos.close()
            }
            throw new RuntimeException(e)
        }
    }

    public void compress(File file, ZipOutputStream out, String basedir) {
        /* 判断是目录还是文件 */
        if (file.isDirectory()) {
            this.compressDirectory(file, out, basedir);
        } else {
            this.compressFile(file, out, basedir);
        }
    }

    /**
     *  压缩目录
     * @param dir
     * @param out
     * @param basedir
     */
    private void compressDirectory(File dir, ZipOutputStream out, String basedir) {
        if (!dir.exists())
            return;

        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            /* 递归 */
            compress(files[i], out, basedir + dir.getName() + "/");
        }
    }

    /**
     * 压缩文件
     * @param file
     * @param out
     * @param basedir
     */
    private void compressFile(File file, ZipOutputStream out, String basedir) {
        if (!file.exists()) {
            return
        }
        BufferedInputStream bis=null
        try {
            bis= new BufferedInputStream(
                    new FileInputStream(file));
            String filePath = (basedir + file.getName())
                    .replaceAll(getOriginalUrl() + "/", "");
            //log.info("basedir:：" + basedir);
            System.out.println(">*压缩文件：" + filePath);
            ZipEntry entry = new ZipEntry(filePath);
            out.putNextEntry(entry);
            int count;
            byte[] data = new byte[BUFFER];
            while ((count = bis.read(data, 0, BUFFER)) != -1) {
                out.write(data, 0, count);
            }
            bis.close();
        } catch (Exception e) {
            if (bis!=null){
                bis.close()
            }
            throw new RuntimeException(e);
        }
    }

    /*public static void main(String[] args) {
        Compressor zc = new Compressor("E:\\Base_Crack.jar");
        zc.compress("E:\\Base\\");
        System.out.println("压缩成功");
    }*/

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }


}