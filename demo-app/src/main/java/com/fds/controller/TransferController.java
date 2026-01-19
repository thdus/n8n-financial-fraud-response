package com.fds.controller;

import com.fds.dto.TransferRequest;
import com.fds.service.TransferService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transfer")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public String transfer(
            @RequestBody TransferRequest request,
            HttpServletRequest httpRequest
    ) {
        transferService.transfer(
                request.userId(),
                request.amount(),
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        );

        return "TRANSFER_REQUESTED";
    }
}
