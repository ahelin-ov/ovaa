package oversecured.ovaa.vulns;

interface ICredentialsService {
    String getPassword();

    String readFile(String path);

    void storeToken(String token);
}
