package org.web3j.spring.autoconfigure;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.spring.actuate.Web3jHealthIndicator;
import org.web3j.spring.autoconfigure.context.SpringApplicationTest;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { Web3jAutoConfiguration.class, SpringApplicationTest.class })
class Web3jHealthIndicatorTest {

    @Autowired
    Web3jHealthIndicator web3jHealthIndicator;

    @Autowired
    Web3j web3j;

    @Test
    void testHealthCheckIndicatorDown() throws Exception {
        mockWeb3jCalls(false, null, null, null, null, null);
        Health health = web3jHealthIndicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);

    }

    @Test
    void testHealthCheckIndicatorUp() throws Exception {

        mockWeb3jCalls(true, "23", "ClientVersion",
                new BigInteger("120"), "protocolVersion", new BigInteger("80"));

        Health health = web3jHealthIndicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("netVersion", "23");
        assertThat(health.getDetails()).containsEntry("clientVersion", "ClientVersion");
        assertThat(health.getDetails()).containsEntry("blockNumber", new BigInteger("120"));
        assertThat(health.getDetails()).containsEntry("protocolVersion", "protocolVersion");
        assertThat(health.getDetails()).containsEntry("netPeerCount", new BigInteger("80"));

    }

    private void mockWeb3jCalls(boolean isListening, String netVersion, String clientVersion,
                                BigInteger blockNumber, String protocolVersion, BigInteger netPeer) throws Exception {

        Mockito.when(web3j.netListening().send().isListening()).thenReturn(isListening);
        if (netVersion != null) {
            Mockito.when(web3j.netVersion().sendAsync()).thenReturn(supplyAsync(() -> {
                NetVersion netVersionObject = new NetVersion();
                netVersionObject.setResult(netVersion);
                return netVersionObject;
            }));
        }
        if (clientVersion != null) {
            Mockito.when(web3j.web3ClientVersion().sendAsync()).thenReturn(supplyAsync(() -> {
                Web3ClientVersion web3ClientVersion = new Web3ClientVersion();
                web3ClientVersion.setResult(clientVersion);
                return web3ClientVersion;
            }));
        }
        if (blockNumber != null) {
            Mockito.when(web3j.ethBlockNumber().sendAsync()).thenReturn(supplyAsync(() -> {
                EthBlockNumber ethBlockNumber = new EthBlockNumber();
                ethBlockNumber.setResult(Numeric.encodeQuantity(blockNumber));
                return ethBlockNumber;
            }));
        }
        if (protocolVersion != null) {
            Mockito.when(web3j.ethProtocolVersion().sendAsync()).thenReturn(supplyAsync(() -> {
                EthProtocolVersion ethProtocolVersion = new EthProtocolVersion();
                ethProtocolVersion.setResult(protocolVersion);
                return ethProtocolVersion;
            }));
        }
        if (netPeer != null) {
            Mockito.when(web3j.netPeerCount().sendAsync()).thenReturn(supplyAsync(() -> {
                NetPeerCount netPeerCount = new NetPeerCount();
                netPeerCount.setResult(Numeric.encodeQuantity(netPeer));
                return netPeerCount;
            }));
        }
    }

}