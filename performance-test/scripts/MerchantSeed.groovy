import net.grinder.script.GTest
import net.grinder.script.Grinder
import net.grinder.scriptengine.groovy.junit.GrinderRunner
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread
import org.junit.Test
import org.junit.runner.RunWith
import org.ngrinder.http.HTTPRequest
import org.ngrinder.http.HTTPResponse
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.nio.charset.StandardCharsets
import java.util.UUID

@RunWith(GrinderRunner)
class MerchantSeed {
    static HTTPRequest http
    static final String BASE_URL = "http://host.docker.internal:8080"
    static final String PW = "Password123!"
    static final String ACC_PW = "1234"

    @BeforeProcess
    static void beforeProcess() {
        http = new HTTPRequest()
    }

    @Test
    void createSuperTargets() {
        def json = new JsonSlurper()

        // --- 슈퍼 가맹점 100개 생성 ---
        (0..100).each { i ->
            String bizNo = "100-45-${10000 + i}"
            String mId = "super_mer_${i}_${UUID.randomUUID().toString().substring(0, 4)}"

            http.POST(BASE_URL + "/api/merchants", JsonOutput.toJson([
                    loginId: mId, password: PW, accountPassword: ACC_PW,
                    name: "상주_${i}", contact: "010-1111-1111",
                    businessNumber: bizNo, merchantName: "슈퍼치킨_${i}호점", category: "FOOD"
            ]).getBytes(StandardCharsets.UTF_8))

            // 나중에 로그인을 위해 ID와 PW를 같이 찍어줍니다
            Grinder.grinder.logger.info("MERCHANT_DATA:${mId},${PW},${bizNo}")
        }
    }
}