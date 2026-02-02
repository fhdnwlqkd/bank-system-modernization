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
class UserSeed {
    HTTPRequest http
    static final String BASE_URL = "http://host.docker.internal:8080"
    static final String PW = "Password123!"
    static final String ACC_PW = "1234"

    @BeforeProcess
    static void beforeProcess() {
        new HTTPRequest()
    }

    @BeforeThread
    void beforeThread() {
        http = new HTTPRequest()
    }

    @Test
    void seedData() {
        def json = new JsonSlurper()
        String id = "user_" + UUID.randomUUID().toString().substring(0, 8)

        // 1. 가입
        http.POST(BASE_URL + "/api/members", JsonOutput.toJson([
                loginId: id, password: PW, name: id, contact: "010-0000-0000"
        ]).getBytes(StandardCharsets.UTF_8))

        // 2. 로그인
        def loginRes = http.POST(BASE_URL + "/api/auth/login", JsonOutput.toJson([
                loginId: id, password: PW
        ]).getBytes(StandardCharsets.UTF_8))
        String token = "Bearer " + json.parseText(loginRes.bodyText).data.accessToken

        // 3. 계좌 생성
        http.setHeaders(["Authorization": token, "Content-Type": "application/json"])
        def accRes = http.POST(BASE_URL + "/api/accounts", JsonOutput.toJson([
                accountPassword: ACC_PW
        ]).getBytes(StandardCharsets.UTF_8))
        def accData = json.parseText(accRes.bodyText).data

        // 4. 입금 (나중에 결제 테스트를 위해 넉넉히)
        http.POST(BASE_URL + "/api/transactions/deposit", JsonOutput.toJson([
                accountNumber: accData.accountNumber, amount: 1000000, idempotencyKey: UUID.randomUUID().toString()
        ]).getBytes(StandardCharsets.UTF_8))

        // 5. 카드 발급
        def cardRes = http.POST(BASE_URL + "/api/cards", JsonOutput.toJson([
                accountNumber: accData.accountNumber, accountPassword: ACC_PW,
                cardPassword: ACC_PW, cardType: "CHECK"
        ]).getBytes(StandardCharsets.UTF_8))
        String cardId = json.parseText(cardRes.bodyText).data.externalId

        // CSV 형식으로 표준 출력
        // 형식: 아이디,비번,계좌번호,계좌ID,카드ID
        Grinder.grinder.logger.info("USER_DATA:${id},${PW},${accData.accountNumber},${accData.externalId},${cardId}")
    }
}