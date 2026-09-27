package com.aolda.itda.service.forwarding;

import com.aolda.itda.dto.forwarding.ForwardingDTO;
import com.aolda.itda.entity.forwarding.Forwarding;
import com.aolda.itda.exception.CustomException;
import com.aolda.itda.exception.ErrorCode;
import com.aolda.itda.repository.forwarding.ForwardingRepository;
import com.aolda.itda.service.AuthService;
import com.aolda.itda.template.ForwardingTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ForwardingProxyProtocolTest {
    private final ForwardingRepository repository = mock(ForwardingRepository.class);
    private final AuthService auth = mock(AuthService.class);
    private final ForwardingTemplate template = mock(ForwardingTemplate.class);
    private final ForwardingService service = new ForwardingService(template, repository, auth);
    private final RuntimeException rendered = new RuntimeException("stop before filesystem and nginx API");

    @BeforeEach void setup() {
        ReflectionTestUtils.setField(service, "allowedIpPrefix", "10.16.");
        when(auth.isAdmin("admin")).thenReturn(true);
        when(repository.findAllUsedServerPortsByIsDeleted(false)).thenReturn(List.of());
    }
    private ForwardingDTO request(Boolean enabled) {
        return ForwardingDTO.builder().name("test").instanceIp("10.16.1.100")
                .instancePort("9000").serverPort("25000").proxyProtocol(enabled).build();
    }
    private Forwarding existing(Boolean enabled) {
        Forwarding f = Forwarding.builder().forwardingId(1L).projectId("project")
                .name("test").instanceIp("10.16.1.100").instancePort("9000")
                .serverPort("25000").proxyProtocol(enabled).isDeleted(false).build();
        when(repository.findByForwardingIdAndIsDeleted(1L, false)).thenReturn(Optional.of(f));
        return f;
    }
    private void stopAtRender() {
        when(template.getPortForwardingWithTCP(anyString(), anyString(), anyString(), anyString(), anyBoolean()))
                .thenThrow(rendered);
    }
    @Test void nonAdminCannotSubmitOptionOnCreate() {
        for (boolean enabled : List.of(true, false)) {
            CustomException ex = assertThrows(CustomException.class,
                    () -> service.createForwarding("project", request(enabled), "user"));
            assertEquals(ErrorCode.UNAUTHORIZED_USER, ex.getErrorCode());
        }
        verify(repository, never()).save(any());
        verifyNoInteractions(template);
    }
    @Test void nonAdminCannotEnableOrDisableOnEdit() {
        Forwarding f = existing(true);
        for (boolean enabled : List.of(true, false)) {
            CustomException ex = assertThrows(CustomException.class,
                    () -> service.editForwarding(1L, ForwardingDTO.builder().proxyProtocol(enabled).build(), List.of("project"), "user"));
            assertEquals(ErrorCode.UNAUTHORIZED_USER, ex.getErrorCode());
        }
        assertTrue(f.getProxyProtocol());
        verifyNoInteractions(template);
    }
    @Test void createDefaultsOffAndAdminCanChooseBothStates() {
        stopAtRender();
        for (String user : List.of("user", "admin")) {
            assertSame(rendered, assertThrows(RuntimeException.class,
                    () -> service.createForwarding("project", request(null), user)));
            verify(repository, atLeastOnce()).save(argThat(f -> Boolean.FALSE.equals(f.getProxyProtocol())));
        }
        for (boolean enabled : List.of(true, false)) {
            assertSame(rendered, assertThrows(RuntimeException.class,
                    () -> service.createForwarding("project", request(enabled), "admin")));
            verify(template, atLeastOnce()).getPortForwardingWithTCP("25000", "10.16.1.100", "9000", "test", enabled);
            verify(repository, atLeastOnce()).save(argThat(f -> Boolean.valueOf(enabled).equals(f.getProxyProtocol())));
        }
    }
    @Test void adminCanToggleAndOmittedPatchPreservesValue() {
        Forwarding f = existing(false);
        stopAtRender();
        for (boolean enabled : List.of(true, false)) {
            assertSame(rendered, assertThrows(RuntimeException.class,
                    () -> service.editForwarding(1L, ForwardingDTO.builder().proxyProtocol(enabled).build(), List.of("project"), "admin")));
            assertEquals(enabled, f.getProxyProtocol());
            verify(template).getPortForwardingWithTCP("25000", "10.16.1.100", "9000", "test", enabled);
        }
        f.edit(ForwardingDTO.builder().proxyProtocol(true).build());
        assertSame(rendered, assertThrows(RuntimeException.class,
                () -> service.editForwarding(1L, ForwardingDTO.builder().name("renamed").build(), List.of("project"), "user")));
        assertTrue(f.getProxyProtocol());
    }
    @Test void legacyNullAndCopyRoundTrip() {
        Forwarding f = existing(null);
        assertFalse(f.toForwardingDTO().getProxyProtocol());
        f.edit(ForwardingDTO.builder().proxyProtocol(true).build());
        assertTrue(new Forwarding(f).toForwardingDTO().getProxyProtocol());
        f.edit(ForwardingDTO.builder().proxyProtocol(false).build());
        assertFalse(f.toForwardingDTO().getProxyProtocol());
    }
    @Test void directiveOnlyAppearsWhenEnabled() {
        ForwardingTemplate actual = new ForwardingTemplate();
        String off = actual.getPortForwardingWithTCP("25000", "10.16.1.100", "9000", "test", false);
        String on = actual.getPortForwardingWithTCP("25000", "10.16.1.100", "9000", "test", true);
        assertFalse(off.contains("proxy_protocol"));
        assertTrue(on.contains(" proxy_protocol on;\n"));
        assertEquals(off, on.replace(" proxy_protocol on;\n", ""));
    }
}
