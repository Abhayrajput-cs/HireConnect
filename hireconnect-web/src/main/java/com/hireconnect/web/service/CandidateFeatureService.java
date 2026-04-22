package com.hireconnect.web.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hireconnect.web.domain.Bookmark;
import com.hireconnect.web.domain.WalletAccount;
import com.hireconnect.web.dto.JobResponse;
import com.hireconnect.web.repository.BookmarkRepository;
import com.hireconnect.web.repository.WalletAccountRepository;
import com.hireconnect.web.support.PortalException;

@Service
@Transactional
public class CandidateFeatureService {

    private final BookmarkRepository bookmarkRepository;
    private final WalletAccountRepository walletAccountRepository;

    public CandidateFeatureService(BookmarkRepository bookmarkRepository, WalletAccountRepository walletAccountRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.walletAccountRepository = walletAccountRepository;
    }

    public void bookmarkJob(Integer profileId, Integer jobId) {
        if (!bookmarkRepository.existsByUserProfileIdAndJobId(profileId, jobId)) {
            Bookmark bookmark = new Bookmark();
            bookmark.setUserProfileId(profileId);
            bookmark.setJobId(jobId);
            bookmarkRepository.save(bookmark);
        }
    }

    public List<Integer> getBookmarkedJobIds(Integer profileId) {
        return bookmarkRepository.findByUserProfileIdOrderByCreatedAtDesc(profileId).stream()
            .map(Bookmark::getJobId)
            .toList();
    }

    public BigDecimal addMoneyToWallet(Integer profileId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new PortalException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }
        WalletAccount walletAccount = walletAccountRepository.findById(profileId).orElseGet(() -> {
            WalletAccount created = new WalletAccount();
            created.setProfileId(profileId);
            created.setBalance(BigDecimal.ZERO);
            return created;
        });
        walletAccount.setBalance(walletAccount.getBalance().add(amount));
        return walletAccountRepository.save(walletAccount).getBalance();
    }

    public BigDecimal getWalletBalance(Integer profileId) {
        return walletAccountRepository.findById(profileId).map(WalletAccount::getBalance).orElse(BigDecimal.ZERO);
    }
}
