import groovyx.net.http.HttpBuilder

class ServerStarted {
    static HttpBuilder loadHttp() {
        ServerScript.main()
        return loadClient()
    }

    private static HttpBuilder loadClient() {
        HttpBuilder http = HttpBuilder.configure {
            request.uri = "http://localhost:${ServerScript.webport}"
        }
        boolean started = false
        int connectionFails = 0
        while (!started) {
            try {
                byte[] result = http.get {
                    request.uri.path = '/alive'
                } as byte[]
                started = Boolean.parseBoolean(new String(result))
            } catch (RuntimeException e) {
                if (!(e.cause instanceof ConnectException) && !(e.cause instanceof SocketException)) {
                    throw e
                }
                assert connectionFails++ < 20
                println "connection fails $connectionFails $e"
                sleep(1000)
            }
        }
        assert started
        println 'server started'
        http
    }
}
