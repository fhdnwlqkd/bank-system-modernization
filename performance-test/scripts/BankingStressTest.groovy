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
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

@RunWith(GrinderRunner)
class BankingStressTest {

    static GTest testPay, testTransfer, testRefund, testHistory, testGetMe, testSettlement

    // 공유 데이터 저장소 (sharedAccountPool 제거)
    static List<Map> userPool = []
    static List<Map> merchantPool = []

    static ConcurrentHashMap<String, String> tokenCache = new ConcurrentHashMap<>()
    static ConcurrentLinkedQueue<Map> refundableQueue = new ConcurrentLinkedQueue<>()

    static final String BASE_URL = "http://host.docker.internal:8080"
    HTTPRequest http

    @BeforeProcess
    static void beforeProcess() {
        testPay = new GTest(1, "ACTION: Payment (45%)")
        testTransfer = new GTest(2, "ACTION: Transfer (30%)")
        testRefund = new GTest(3, "ACTION: Refund (5%)")
        testHistory = new GTest(4, "ACTION: History (8%)")
        testGetMe = new GTest(5, "ACTION: UserMe (10%)")
        testSettlement = new GTest(6, "ACTION: MerchantSettlement (2%)")

        new HTTPRequest()

        // CSV 로드 (shared_accounts 제거)
        loadCsv("users.csv", userPool)
        loadCsv("merchants.csv", merchantPool)

        Grinder.grinder.logger.info("리소스 로드 완료: 유저 ${userPool.size()}명, 가맹점 ${merchantPool.size()}개")
    }

    @BeforeThread
    void beforeThread() {
        this.http = new HTTPRequest()
    }

    @Test
    void stressScenario() {
        def json = new JsonSlurper()
        def rnd = new Random()
        int dice = rnd.nextInt(100)

        if (dice < 2) {
            doMerchantSettlement(rnd, json)
            return
        }

        def user = userPool[rnd.nextInt(userPool.size())]

        String token = tokenCache.computeIfAbsent(user.loginId) { id ->
            return loginAndGetToken(id, user.password, json)
        }

        if (dice < 47) {
            doPayment(user, token, rnd, json)
        } else if (dice < 77) {
            doTransfer(user, token, rnd)
        } else if (dice < 82) {
            doRefund(token, json)
        } else if (dice < 90) {
            doHistory(user, token)
        } else {
            doGetMe(token)
        }
    }

    /* ============================================================
       ACTIONS
    ============================================================ */

    private void doPayment(Map user, String token, Random rnd, JsonSlurper json) {
        testPay.record(http)
        def merchant = merchantPool[rnd.nextInt(merchantPool.size())]
        String key = UUID.randomUUID().toString()

        def res = postWithAuth(token, "/api/cards/pay", [
                cardExternalId: user.cardId, amount: 2000, password: "1234",
                businessNumber: merchant.businessNumber, idempotencyKey: key
        ])

        if (res.statusCode == 401 || res.statusCode == 403) {
            tokenCache.remove(user.loginId)
            ensureStatus(res, "Auth_Failure", [200])
        }

        ensureStatus(res, "Payment", [200, 201])

        if (res.statusCode < 300) {
            def payId = json.parseText(res.bodyText).data.paymentExternalId
            refundableQueue.add([token: token, payId: payId])
        }

        if (rnd.nextInt(100) < 2) {
            def resDup = postWithAuth(token, "/api/cards/pay", [
                    cardExternalId: user.cardId, amount: 2000, password: "1234",
                    businessNumber: merchant.businessNumber, idempotencyKey: key
            ])
            ensureStatus(resDup, "Payment_Idempotency", [200, 201, 409])
        }
    }

    private void doTransfer(Map user, String token, Random rnd) {
        testTransfer.record(http)
        def target = userPool[rnd.nextInt(userPool.size())]

        while (target.loginId == user.loginId) {
            target = userPool[rnd.nextInt(userPool.size())]
        }

        def res = postWithAuth(token, "/api/transactions/transfer", [
                fromAccountNumber: user.accountNo,
                toAccountNumber: target.accountNo, // target 유저의 계좌번호 사용
                amount: 1000,
                accountPassword: "1234",
                idempotencyKey: UUID.randomUUID().toString()
        ])
        ensureStatus(res, "Transfer", [200, 201])
    }

    private void doRefund(String token, JsonSlurper json) {
        def payData = refundableQueue.poll()
        if (!payData) return

        testRefund.record(http)
        def res = postWithAuth(payData.token, "/api/cards/refund", [
                paymentExternalId: payData.payId, amount: 1000,
                reason: "부하테스트 환불", idempotencyKey: UUID.randomUUID().toString()
        ])
        ensureStatus(res, "Refund", [200, 404, 409])
    }

    private void doMerchantSettlement(Random rnd, JsonSlurper json) {
        testSettlement.record(http)
        def merchant = merchantPool[rnd.nextInt(merchantPool.size())]
        String mToken = loginAndGetToken(merchant.loginId, merchant.password, json)

        String today = java.time.LocalDate.now().toString()
        def res = getWithAuth(mToken, "/api/merchants/me/settlements?from=${today}&to=${today}")
        ensureStatus(res, "Settlement", [200])
    }

    private void doHistory(Map user, String token) {
        testHistory.record(http)
        String day = java.time.LocalDate.now().toString()
        def res = getWithAuth(token, "/api/transactions/history?accountExternalId=${user.accountId}&startDate=${day}&endDate=${day}&page=0&size=10")
        ensureStatus(res, "History", [200])
    }

    private void doGetMe(String token) {
        testGetMe.record(http)
        def res = getWithAuth(token, "/api/members/me")
        ensureStatus(res, "GetMe", [200])
    }

    /* ============================================================
       HELPERS: 공통 유틸리티 및 검증
    ============================================================ */

    private void ensureStatus(HTTPResponse res, String label, List allowed) {
        String serverMessage = ""
        try {
            def body = new JsonSlurper().parseText(res.bodyText)
            if (body.error != null) {
                serverMessage = body.error.message ?: ""
            } else if (body.message != null) {
                serverMessage = body.message
            }
        } catch (Exception e) {
            serverMessage = "JSON 파싱 불가 또는 메시지 없음"
        }

        if (res.statusCode >= 500) {
            String errorLog = "[SYSTEM ERROR] ${label} | Code: ${res.statusCode} | Msg: ${serverMessage}"
            Grinder.grinder.logger.error("${errorLog} | Body: ${res.bodyText}")
            throw new AssertionError(errorLog)
        }

        if (!allowed.contains(res.statusCode)) {
            String warnLog = "[BUSINESS ERROR] ${label} | Code: ${res.statusCode} | Msg: ${serverMessage}"
            Grinder.grinder.logger.warn(warnLog)
            throw new AssertionError(warnLog)
        }
    }

    private String loginAndGetToken(String id, String pw, JsonSlurper json) {
        def res = http.POST(BASE_URL + "/api/auth/login",
                JsonOutput.toJson([loginId: id, password: pw]).getBytes(StandardCharsets.UTF_8),
                ["Content-Type": "application/json"])

        if (res.statusCode != 200) {
            throw new RuntimeException("로그인 실패: ${id} | Code: ${res.statusCode}")
        }
        return "Bearer " + json.parseText(res.bodyText).data.accessToken
    }

    private HTTPResponse postWithAuth(String token, String path, Map body) {
        http.setHeaders(["Authorization": token, "Content-Type": "application/json"])
        return http.POST(BASE_URL + path, JsonOutput.toJson(body).getBytes(StandardCharsets.UTF_8))
    }

    private HTTPResponse getWithAuth(String token, String path) {
        http.setHeaders(["Authorization": token])
        return http.GET(BASE_URL + path)
    }

    private static void loadCsv(String fileName, List pool) {
        def file = new File("./resources/${fileName}")
        if (!file.exists()) throw new RuntimeException("${fileName} 파일이 리소스 폴더에 없습니다.")

        file.eachLine { line, idx ->
            if (idx == 1) return
            def c = line.split(',')
            if (fileName == "users.csv") pool << [loginId: c[0], password: c[1], accountNo: c[2], accountId: c[3], cardId: c[4]]
            else if (fileName == "merchants.csv") pool << [loginId: c[0], password: c[1], businessNumber: c[2]]
        }
    }
}