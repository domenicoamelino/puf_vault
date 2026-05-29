import hashlib

PASSWORD_CHARS = (
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    "abcdefghijklmnopqrstuvwxyz"
    "0123456789"
    "!@#$%^&*()-_=+"
)
POLICY_ID = "DEFAULT_16"
PASSWORD_LENGTH = 16


def generate_password(puf_buffer: bytes, user_id: str, service_id: str, creation_nonce: str, version: int) -> str:
    digest = hashlib.sha256(
        puf_buffer
        + user_id.encode()
        + service_id.encode()
        + POLICY_ID.encode()
        + version.to_bytes(4, "little", signed=False)
        + creation_nonce.encode()
    ).digest()
    return "".join(PASSWORD_CHARS[digest[i % 32] % len(PASSWORD_CHARS)] for i in range(PASSWORD_LENGTH))


def is_valid_service_id(service_id: str) -> bool:
    if len(service_id) == 0 or len(service_id) >= 31:
        return False
    return all(is_safe_metadata_char(c) for c in service_id)


def is_valid_creation_nonce(creation_nonce: str) -> bool:
    if len(creation_nonce) == 0 or len(creation_nonce) >= 48:
        return False
    return all(is_safe_metadata_char(c) for c in creation_nonce)


def is_safe_metadata_char(c: str) -> bool:
    allowed = set("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._-")
    return c in allowed


def test_password_is_deterministic_for_same_inputs():
    puf = bytes(range(64))
    p1 = generate_password(puf, "user001", "github.com", "20260526T163012Z_9f2c1d3a", 0)
    p2 = generate_password(puf, "user001", "github.com", "20260526T163012Z_9f2c1d3a", 0)
    assert p1 == p2
    assert len(p1) == 16


def test_rotation_changes_password_output():
    puf = bytes([7] * 64)
    p1 = generate_password(puf, "user001", "bank-app", "20260526T163012Z_9f2c1d3a", 0)
    p2 = generate_password(puf, "user001", "bank-app", "20260526T163012Z_9f2c1d3a", 1)
    assert p1 != p2


def test_user_or_service_change_changes_password_output():
    puf = bytes([9] * 64)
    base = generate_password(puf, "user001", "mail", "20260526T163012Z_9f2c1d3a", 0)
    assert base != generate_password(puf, "test001", "mail", "20260526T163012Z_9f2c1d3a", 0)
    assert base != generate_password(puf, "user001", "mail2", "20260526T163012Z_9f2c1d3a", 0)


def test_service_id_validation_matches_firmware_rules():
    assert is_valid_service_id("github.com")
    assert is_valid_service_id("a_b-c.1")
    assert not is_valid_service_id("")
    assert not is_valid_service_id("invalid space")
    assert not is_valid_service_id("x" * 31)


def test_creation_nonce_change_changes_password_output():
    puf = bytes([3] * 64)
    p1 = generate_password(puf, "user001", "github.com", "20260526T163012Z_9f2c1d3a", 0)
    p2 = generate_password(puf, "user001", "github.com", "20260526T163099Z_deadbeef", 0)
    assert p1 != p2


def test_creation_nonce_validation_matches_firmware_rules():
    assert is_valid_creation_nonce("20260526T163012Z_9f2c1d3a")
    assert is_valid_creation_nonce("abc.DEF-123_456")
    assert not is_valid_creation_nonce("")
    assert not is_valid_creation_nonce("invalid space")
    assert not is_valid_creation_nonce("x" * 48)
