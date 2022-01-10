package com.pluralsight.pension.investment;

import com.pluralsight.pension.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalInvestmentManagementServiceTest {

    @Spy
    private ExternalInvestmentManagementService underTest;



/**    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

MockitoExtension.class  class seviyesi ekledik ve pom.xml e  <artifactId>mockito-junit-jupiter</artifactId> dependencies ekledik o yuden
 initMocks u initiliaze etmeye gerek yok
*/

@Test
public void shouldBeAbleToBuyPensionFundInvestmentIfEnoughCashInAccount() throws IOException {

//    when(underTest.executeInvestmentTransaction(anyString(), any(BigDecimal.class), anyString()))
//            .thenReturn(true);           //java.lang.NullPointerException

    doReturn(true).when(underTest).executeInvestmentTransaction(anyString(), any(BigDecimal.class), anyString());

    Account testAccount = new Account();
    testAccount.setInvestments(new HashSet<>());  // The Set is a collection with no duplicates
    final BigDecimal startingAccountBalance = new BigDecimal(1000000);
    testAccount.setAvailableCash(startingAccountBalance);
    final BigDecimal desiredInvestmentAmount = new BigDecimal(100000);

    underTest.buyInvestmentFund(testAccount, "TEST_FUND_ID", desiredInvestmentAmount);

    assertEquals(testAccount.getAvailableCash(), startingAccountBalance.subtract(desiredInvestmentAmount));
    assertTrue(testAccount.getInvestments().contains("TEST_FUND_ID"));


}



}