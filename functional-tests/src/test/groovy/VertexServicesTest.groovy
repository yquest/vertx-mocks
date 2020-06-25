import groovyx.net.http.HttpBuilder
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class VertexServicesTest extends Specification {
    static private HttpBuilder http

    def setupSpec() {
        http = ServerStarted.loadHttp()
    }

    def cleanupSpec() {
        http.close()
    }

    private static void undeployDependencies(String service, List<String> dependencies) {
        def deployedServices = http.get {
            request.uri.path = '/deployed'
        } as Map<String, String>

        for (serviceDependency in dependencies) {
            if (deployedServices.containsKey(serviceDependency)) {
                println "undeploy $serviceDependency dependency of $service"
            } else {
                println "not deployed $serviceDependency dependency of $service"
            }
        }
        if (deployedServices.containsKey(service)) {
            println "undeploy $service"
        }
    }

    def "deploy service #service"() {
        undeployDependencies(service, dependencies)

        def result = http.get {
            request.uri.path = "/deploy/$service"
        } as Map<String, String>

        def deployedServices = http.get {
            request.uri.path = '/deployed'
        } as Map<String, String>

        expect:
        deployedServices[service] == result.id

        where:
        service        | dependencies
        'server'       | ['java-route', 'groovy-route']
        'java-route'   | []
        'groovy-route' | []
    }

    private static String deployAndGetId(String service, List<String> dependencies) {
        def deployedServices = http.get {
            request.uri.path = '/deployed'
        } as Map<String, String>

        for (serviceDependency in dependencies) {
            if (!deployedServices.containsKey(serviceDependency)) {
                http.get {
                    request.uri.path = "/deploy/$serviceDependency"
                } as Map<String, String>
            }
        }
        String serviceId
        if (!deployedServices.containsKey(service)) {
            serviceId = (http.get {
                request.uri.path = "/deploy/$service"
            } as Map<String, String>).id
        } else {
            serviceId = deployedServices[service]
        }
        return serviceId
    }

    def "undeploy service #service"() {
        given:
        http.get {
            request.uri.path = "/undeploy/$service"
        }

        def deployedServices = http.get {
            request.uri.path = '/deployed'
        } as Map<String, String>

        expect:
        !deployedServices.containsKey(serviceId)

        where:
        service        | serviceId
        'java-route'   | deployAndGetId(service, ['server'])
        'groovy-route' | deployAndGetId(service, ['server'])
        'server'       | deployAndGetId(service, [])
    }

}