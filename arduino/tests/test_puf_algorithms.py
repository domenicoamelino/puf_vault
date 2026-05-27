import hashlib

PASSWORD_CHARS = (
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    "abcdefghijklmnopqrstuvwxyz"
    "0123456789"
    "!@#$%^&*()-_=+"
)
POLICY_ID = "DEFAULT_16"
PASSWORD_LENGTH = 16


def generate_password(puf_buffer: bytes, user_id: str, service_id: str, version: int) -> str:
    digest = hashlib.sha256(
        puf_buffer
        + user_id.encode()
        + service_id.encode()
        + POLICY_ID.encode()
        + version.to_bytes(4, "little", signed=False)
    ).digest()
    return "".join(PASSWORD_CHARS[digest[i % 32] % len(PASSWORD_CHARS)] for i in range(PASSWORD_LENGTH))


def is_valid_service_id(service_id: str) -> bool:
    if len(service_id) == 0 or len(service_id) >= 31:
        return False
    allowed = set("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._-")
    return all(c in allowed for c in service_id)


def test_password_is_deterministic_for_same_inputs():
    puf = bytes(range(64))
    p1 = generate_password(puf, "user001", "github.com", 0)
    p2 = generate_password(puf, "user001", "github.com", 0)
    assert p1 == p2
    assert len(p1) == 16


def test_rotation_changes_password_output():
    puf = bytes([7] * 64)
    p1 = generate_password(puf, "user001", "bank-app", 0)
    p2 = generate_password(puf, "user001", "bank-app", 1)
    assert p1 != p2


def test_user_or_service_change_changes_password_output():
    puf = bytes([9] * 64)
    base = generate_password(puf, "user001", "mail", 0)
    assert base != generate_password(puf, "test001", "mail", 0)
    assert base != generate_password(puf, "user001", "mail2", 0)


def test_service_id_validation_matches_firmware_rules():
    assert is_valid_service_id("github.com")
    assert is_valid_service_id("a_b-c.1")
    assert not is_valid_service_id("")
    assert not is_valid_service_id("invalid space")
    assert not is_valid_service_id("x" * 31)
