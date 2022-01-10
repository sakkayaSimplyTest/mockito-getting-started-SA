package com.pluralsight.pension.setup;

import com.pluralsight.pension.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;

import java.io.IOException;
import java.time.LocalDate;

import static com.pluralsight.pension.setup.AccountOpeningService.UNACCEPTABLE_RISK_PROFILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

class AccountOpeningServiceTest {

    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Smith";
    private static final LocalDate DOB = LocalDate.of(1990, 1, 1);
    private static final String TAX_ID = "123XYZ9";
    public final String ACCOUNT_ID = "some_id";
    private AccountOpeningService underTest;
    private BackgroundCheckService backgroundCheckService = mock(BackgroundCheckService.class);
    private ReferenceIdsManager referenceIdsManager = mock(ReferenceIdsManager.class);
    private AccountRepository accountRepository = mock(AccountRepository.class);
    private AccountOpeningEventPublisher eventPublisher = mock(AccountOpeningEventPublisher.class);


    @BeforeEach
    void setUp() {
        underTest = new AccountOpeningService(backgroundCheckService,referenceIdsManager,accountRepository, eventPublisher);
    }
    /**
     Mockito Argument Matcher – eq()
     When we use argument matchers, then all the arguments should use matchers. If we want to use a specific value for an argument, then we can use eq() method.
     */
    @Test
    public void shouldOpenAccount() throws IOException {
        final BackgroundCheckResults okBackgroundCheckResults = new BackgroundCheckResults("something_not_unacceptable", 100);
        given(backgroundCheckService.confirm(FIRST_NAME, LAST_NAME, TAX_ID, DOB))
                .willReturn(okBackgroundCheckResults);

        given(referenceIdsManager.obtainId(eq(FIRST_NAME), anyString(), eq(LAST_NAME), eq(TAX_ID), eq(DOB)))
                .willReturn(ACCOUNT_ID);

        final AccountOpeningStatus accountOpeningStatus = underTest.openAccount(FIRST_NAME, LAST_NAME, TAX_ID, DOB);
        assertEquals(AccountOpeningStatus.OPENED, accountOpeningStatus);

        ArgumentCaptor<BackgroundCheckResults> backgroundCheckResultsArgumentCaptor = ArgumentCaptor.forClass(BackgroundCheckResults.class);

        then(accountRepository).should().save(eq(ACCOUNT_ID), eq(FIRST_NAME), eq(LAST_NAME), eq(TAX_ID), eq(DOB), backgroundCheckResultsArgumentCaptor.capture());
//        verify(eventPublisher).notify(ACCOUNT_ID);
        then(eventPublisher).should().notify(anyString());
        System.out.println(backgroundCheckResultsArgumentCaptor.getValue().getRiskProfile() + " " +
                backgroundCheckResultsArgumentCaptor.getValue().getUpperAccountLimit());
        assertEquals(okBackgroundCheckResults.getRiskProfile(), backgroundCheckResultsArgumentCaptor.getValue().getRiskProfile());
        assertEquals(okBackgroundCheckResults.getUpperAccountLimit(), backgroundCheckResultsArgumentCaptor.getValue().getUpperAccountLimit());
        verifyNoMoreInteractions(ignoreStubs(backgroundCheckService, referenceIdsManager));

//        verifyNoMoreInteractions(accountRepository, eventPublisher);
        //BDD VERSION
        then(accountRepository).shouldHaveNoMoreInteractions();
        then(eventPublisher).shouldHaveNoMoreInteractions();

//        verify(backgroundCheckService).confirm(FIRST_NAME, LAST_NAME, TAX_ID, DOB);
//        verify(referenceIdsManager).obtainId(eq(FIRST_NAME), anyString(), eq(LAST_NAME), eq(TAX_ID), eq(DOB));
    }

    /** We now have a business logic failure, rather than a failure caused by a technical exception.
             Therefore, using mocks now allows your test to run fully and reach a result
    */


    @Test
    public void shouldDeclineAccountIfUnacceptableRiskProfileBackgroundCheckResponseReceived() throws IOException {
        when(backgroundCheckService.confirm(FIRST_NAME, LAST_NAME, TAX_ID, DOB))
                .thenReturn(new BackgroundCheckResults(UNACCEPTABLE_RISK_PROFILE, 0));
        final AccountOpeningStatus accountOpeningStatus = underTest.openAccount(FIRST_NAME, LAST_NAME, TAX_ID, DOB);
        assertEquals(AccountOpeningStatus.DECLINED, accountOpeningStatus);
    }

    @Test
    public void shouldDeclineAccountIfNullBackgroundCheckResponseReceived() throws IOException {
        when(backgroundCheckService.confirm(FIRST_NAME, LAST_NAME, TAX_ID, DOB))
                .thenReturn(null);
        final AccountOpeningStatus accountOpeningStatus = underTest.openAccount(FIRST_NAME, LAST_NAME, TAX_ID, DOB);
        assertEquals(AccountOpeningStatus.DECLINED, accountOpeningStatus);
    }

    @Test         //CHECKED EXCEPTION
    public void shouldThrowIfBackgroundChecksServiceThrows() throws IOException {
        when(backgroundCheckService.confirm(FIRST_NAME, LAST_NAME, TAX_ID, DOB))
                .thenThrow(new IOException());
        assertThrows(IOException.class, () -> underTest.openAccount(FIRST_NAME, LAST_NAME, TAX_ID, DOB));
    }


    @Test    //UNCHECKED EXCEPTION  -RUNTIME Exception rather than catching it
    public void shouldThrowIfReferenceIdsManagerThrows() throws IOException {
        when(backgroundCheckService.confirm(FIRST_NAME, LAST_NAME, TAX_ID, DOB))
                .thenReturn(new BackgroundCheckResults("something_not_unacceptable", 100));
        when(referenceIdsManager.obtainId(eq(FIRST_NAME), anyString(), eq(LAST_NAME), eq(TAX_ID), eq(DOB)))
                .thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> underTest.openAccount(FIRST_NAME, LAST_NAME, TAX_ID, DOB));

    }


    @Test
    public void shouldThrowIfAccountRepositoryThrows() throws IOException {
        final BackgroundCheckResults backgroundCheckResults = new BackgroundCheckResults("something_not_unacceptable", 100);
        when(backgroundCheckService.confirm(FIRST_NAME, LAST_NAME, TAX_ID, DOB))
                .thenReturn(backgroundCheckResults);
        when(referenceIdsManager.obtainId(eq(FIRST_NAME), anyString(), eq(LAST_NAME), eq(TAX_ID), eq(DOB)))
                .thenReturn("someID");
        when(accountRepository.save("someID", FIRST_NAME, LAST_NAME, TAX_ID, DOB, backgroundCheckResults))
                .thenThrow(new RuntimeException());
        assertThrows(RuntimeException.class, () -> underTest.openAccount(FIRST_NAME, LAST_NAME, TAX_ID, DOB));
    }

    @Test
    public void shouldThrowIfEventPublisherThrows() throws IOException {

        final BackgroundCheckResults backgroundCheckResults = new BackgroundCheckResults("something_not_unacceptable", 100);
        when(backgroundCheckService.confirm(FIRST_NAME, LAST_NAME, TAX_ID, DOB))
                .thenReturn(backgroundCheckResults);
        final String accountID = "someID";
        when(referenceIdsManager.obtainId(eq(FIRST_NAME), anyString(), eq(LAST_NAME), eq(TAX_ID), eq(DOB)))
                .thenReturn(accountID);
        when(accountRepository.save(accountID, FIRST_NAME, LAST_NAME, TAX_ID, DOB, backgroundCheckResults))
                .thenReturn(true);
//        when(eventPublisher.notify(accountID)).thenThrow(new RuntimeException());      // boolean methods
        doThrow(new RuntimeException()).when(eventPublisher).notify(accountID);  // void methods
       assertThrows(RuntimeException.class, () -> underTest.openAccount(FIRST_NAME, LAST_NAME, TAX_ID, DOB));

    }



}