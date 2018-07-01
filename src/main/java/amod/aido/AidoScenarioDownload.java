/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

/* package */ enum AidoScenarioDownload {

    ;

    /* package */ static void download(String tag, String saveDir) {

        // // String fileURL =
        // //
        // "https://www.ethz.ch/content/dam/ethz/special-interest/mavt/dynamic-systems-n-control/idsc-dam/Research_Frazzoli/AMoDeusScenarioSanFrancisco.zip";
        // String fileURL = null;
        //
        // if (tag.equals("SanFrancisco")) {
        // fileURL = "https://polybox.ethz.ch/index.php/s/C3QUuk3cuWWSGmy/download";
        // }
        // if (tag.equals("Berlin")) {
        // fileURL = "https://polybox.ethz.ch/index.php/s/C3QUuk3cuWWSGmy/download";
        // }
        // if (tag.equals("SantiagoDeChile")) {
        // fileURL = "https://polybox.ethz.ch/index.php/s/C3QUuk3cuWWSGmy/download";
        // }
        // if (tag.equals("TelAviv")) {
        // fileURL = "https://polybox.ethz.ch/index.php/s/C3QUuk3cuWWSGmy/download";
        // }
        // HttpDownloader.download(fileURL).to(saveDir);

        // TODO unzip and move into saveDir
    }

    public static void main(String[] args) {
        download("SanFrancisco", "/home/clruch");
    }

}

//// TODO download the right scenario
//
// TrustManager[] trustAllCerts = new TrustManager[]{
// new X509TrustManager() {
// public java.security.cert.X509Certificate[] getAcceptedIssuers() {
// return null;
// }
// public void checkClientTrusted(
// java.security.cert.X509Certificate[] certs, String authType) {
// }
// public void checkServerTrusted(
// java.security.cert.X509Certificate[] certs, String authType) {
// }
// }
// };
//
// // Activate the new trust manager
// try {
// SSLContext sc = SSLContext.getInstance("SSL");
// sc.init(null, trustAllCerts, new java.security.SecureRandom());
// HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
// } catch (Exception e) {
// }
//
// // And as before now you can use URL and URLConnection
// URL url = new
//// URL("https://www.ethz.ch/content/dam/ethz/special-interest/mavt/dynamic-systems-n-control/idsc-dam/Research_Frazzoli/AMoDeusScenarioSanFrancisco.zip");
// URLConnection connection = url.openConnection();
//
// InputStream is = connection.getInputStream();
