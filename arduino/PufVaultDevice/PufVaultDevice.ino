#include <EEPROM.h>
#include <SHA256.h>

#define BAUD_RATE 115200

#define MAX_SLOTS 5
#define PUF_SIZE_BYTES 64
#define PASSWORD_LENGTH 16
#define EEPROM_MAGIC 0x63
#define CREATION_NONCE_SIZE 32

const char POLICY_ID[] = "DEFAULT_16";

const char PASSWORD_CHARS[] =
  "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
  "abcdefghijklmnopqrstuvwxyz"
  "0123456789"
  "!@#$%^&*()-_=+";

#define CHARSET_SIZE (sizeof(PASSWORD_CHARS) - 1)

SHA256 sha256;

__attribute__((section(".noinit")))
uint8_t puf_buffer[PUF_SIZE_BYTES];

struct ServiceSlot {
  bool active;
  char ownerUserId[16];
  char serviceId[32];
  char creationNonce[CREATION_NONCE_SIZE];
  uint32_t version;
};

struct DeviceStorage {
  uint8_t magic;
  ServiceSlot slots[MAX_SLOTS];
};

DeviceStorage storage;
bool powerCycleRequired = false;

void handleCommand(const String& command);
void capturePuf();
void loadStorage();
void saveStorage();
bool isValidUser(const String& userId);
int maxSlotsForUser(const String& userId);
int countUserServices(const char* userId);
int findFreeSlot();
int findService(const char* userId, const char* serviceId);
bool isValidServiceId(const String& serviceId);
bool isValidCreationNonce(const String& creationNonce);
bool isSafeMetadataChar(char c);
String generatePassword(const char* userId, const char* serviceId, const char* creationNonce, uint32_t version);
String getArg(const String& input, int index);
bool isCommandSeparator(char c);

void setup() {
  Serial.begin(BAUD_RATE);
  delay(500);

  capturePuf();
  loadStorage();

  Serial.println(F("PUFVAULT_ARDUINO_READY"));
}

void loop() {
  if (Serial.available() <= 0) return;

  String command = Serial.readStringUntil('\n');
  command.trim();

  if (command.length() == 0) return;

  handleCommand(command);
}

void handleCommand(const String& command) {
  String op = getArg(command, 0);

  if (op == "PING") {
    Serial.println(F("OK PONG"));
    return;
  }

  if (op == "STATUS") {
    if (powerCycleRequired) {
      Serial.println(F("NOK POWER_CYCLE_REQUIRED"));
    } else {
      Serial.println(F("OK READY"));
    }
    return;
  }

  if (op == "CAPABILITY") {
    Serial.println(F("OK USERS=2 TOTAL_SLOTS=5 TEST_SLOTS=2 PERSONAL_SLOTS=3 POLICY=DEFAULT_16"));
    return;
  }

  if (op == "LIST_SERVICES") {
    String userId = getArg(command, 1);

    Serial.println(F("SERVICES_BEGIN"));

    for (int i = 0; i < MAX_SLOTS; i++) {
      if (!storage.slots[i].active) continue;
      if (strcmp(storage.slots[i].ownerUserId, userId.c_str()) != 0) continue;

      Serial.print(F("SLOT "));
      Serial.print(i);
      Serial.print(F(" ACTIVE "));
      Serial.print(storage.slots[i].serviceId);
      Serial.print(F(" VERSION "));
      Serial.println(storage.slots[i].version);
    }

    Serial.println(F("SERVICES_END"));
    return;
  }

  if (powerCycleRequired) {
    Serial.println(F("NOK POWER_CYCLE_REQUIRED"));
    return;
  }

  if (op == "ADD_SERVICE") {
    String userId = getArg(command, 1);
    String serviceId = getArg(command, 2);
    String creationNonce = getArg(command, 3);

    if (!isValidUser(userId)) {
      Serial.println(F("NOK INVALID_USER"));
      return;
    }

    if (!isValidServiceId(serviceId)) {
      Serial.println(F("NOK INVALID_SERVICE_ID"));
      return;
    }

    if (!isValidCreationNonce(creationNonce)) {
      Serial.println(F("NOK INVALID_CREATION_NONCE"));
      return;
    }

    if (findService(userId.c_str(), serviceId.c_str()) >= 0) {
      Serial.println(F("NOK SERVICE_EXISTS"));
      return;
    }

    if (countUserServices(userId.c_str()) >= maxSlotsForUser(userId)) {
      Serial.println(F("NOK USER_SLOT_LIMIT_REACHED"));
      return;
    }

    int freeSlot = findFreeSlot();

    if (freeSlot < 0) {
      Serial.println(F("NOK NO_FREE_SLOT"));
      return;
    }

    memset(&storage.slots[freeSlot], 0, sizeof(ServiceSlot));

    storage.slots[freeSlot].active = true;
    storage.slots[freeSlot].version = 0;

    strncpy(storage.slots[freeSlot].ownerUserId, userId.c_str(), sizeof(storage.slots[freeSlot].ownerUserId) - 1);
    strncpy(storage.slots[freeSlot].serviceId, serviceId.c_str(), sizeof(storage.slots[freeSlot].serviceId) - 1);
    strncpy(storage.slots[freeSlot].creationNonce, creationNonce.c_str(), sizeof(storage.slots[freeSlot].creationNonce) - 1);

    saveStorage();

    Serial.print(F("OK SERVICE_ADDED SLOT="));
    Serial.println(freeSlot);
    return;
  }

  if (op == "DELETE_SERVICE") {
    String userId = getArg(command, 1);
    String serviceId = getArg(command, 2);

    int index = findService(userId.c_str(), serviceId.c_str());

    if (index < 0) {
      Serial.println(F("NOK SERVICE_NOT_FOUND"));
      return;
    }

    memset(&storage.slots[index], 0, sizeof(ServiceSlot));
    saveStorage();

    powerCycleRequired = true;

    Serial.println(F("OK SERVICE_DELETED POWER_CYCLE_REQUIRED"));
    return;
  }

  if (op == "GENERATE_PASSWORD") {
    String userId = getArg(command, 1);
    String serviceId = getArg(command, 2);

    int index = findService(userId.c_str(), serviceId.c_str());

    if (index < 0) {
      Serial.println(F("NOK SERVICE_NOT_FOUND"));
      return;
    }

    String password = generatePassword(
      storage.slots[index].ownerUserId,
      storage.slots[index].serviceId,
      storage.slots[index].creationNonce,
      storage.slots[index].version
    );

    Serial.print(F("OK PASSWORD "));
    Serial.println(password);
    return;
  }

  if (op == "ROTATE_SERVICE") {
    String userId = getArg(command, 1);
    String serviceId = getArg(command, 2);

    int index = findService(userId.c_str(), serviceId.c_str());

    if (index < 0) {
      Serial.println(F("NOK SERVICE_NOT_FOUND"));
      return;
    }

    storage.slots[index].version++;
    saveStorage();

    Serial.print(F("OK SERVICE_ROTATED VERSION="));
    Serial.println(storage.slots[index].version);
    return;
  }

  if (op == "WIPE_ALL") {
    memset(&storage, 0, sizeof(storage));
    storage.magic = EEPROM_MAGIC;
    saveStorage();

    powerCycleRequired = true;

    Serial.println(F("OK WIPED POWER_CYCLE_REQUIRED"));
    return;
  }

  Serial.println(F("NOK UNKNOWN_COMMAND"));
}

void capturePuf() {
  uint8_t* sram = (uint8_t*)0x0100;

  for (int i = 0; i < PUF_SIZE_BYTES; i++) {
    puf_buffer[i] = sram[i];
  }
}

void loadStorage() {
  EEPROM.get(0, storage);

  if (storage.magic != EEPROM_MAGIC) {
    memset(&storage, 0, sizeof(storage));
    storage.magic = EEPROM_MAGIC;
    saveStorage();
  }
}

void saveStorage() {
  EEPROM.put(0, storage);
}

bool isValidUser(const String& userId) {
  return userId == "user001" || userId == "test001";
}

int maxSlotsForUser(const String& userId) {
  if (userId == "test001") return 2;
  if (userId == "user001") return 3;
  return 0;
}

int countUserServices(const char* userId) {
  int count = 0;

  for (int i = 0; i < MAX_SLOTS; i++) {
    if (!storage.slots[i].active) continue;
    if (strcmp(storage.slots[i].ownerUserId, userId) == 0) count++;
  }

  return count;
}

int findFreeSlot() {
  for (int i = 0; i < MAX_SLOTS; i++) {
    if (!storage.slots[i].active) return i;
  }

  return -1;
}

int findService(const char* userId, const char* serviceId) {
  for (int i = 0; i < MAX_SLOTS; i++) {
    if (!storage.slots[i].active) continue;

    if (
      strcmp(storage.slots[i].ownerUserId, userId) == 0 &&
      strcmp(storage.slots[i].serviceId, serviceId) == 0
    ) {
      return i;
    }
  }

  return -1;
}

bool isValidServiceId(const String& serviceId) {
  if (serviceId.length() == 0) return false;
  if (serviceId.length() >= 31) return false;

  for (int i = 0; i < serviceId.length(); i++) {
    char c = serviceId.charAt(i);

    bool valid = isSafeMetadataChar(c);

    if (!valid) return false;
  }

  return true;
}

bool isValidCreationNonce(const String& creationNonce) {
  if (creationNonce.length() == 0) return false;
  if (creationNonce.length() >= CREATION_NONCE_SIZE) return false;

  for (int i = 0; i < creationNonce.length(); i++) {
    char c = creationNonce.charAt(i);

    if (!isSafeMetadataChar(c)) return false;
  }

  return true;
}

bool isSafeMetadataChar(char c) {
  return
    (c >= 'A' && c <= 'Z') ||
    (c >= 'a' && c <= 'z') ||
    (c >= '0' && c <= '9') ||
    c == '.' ||
    c == '-' ||
    c == '_';
}

String generatePassword(const char* userId, const char* serviceId, const char* creationNonce, uint32_t version) {
  uint8_t hash[32];

  sha256.reset();

  sha256.update(puf_buffer, PUF_SIZE_BYTES);
  sha256.update((const uint8_t*)userId, strlen(userId));
  sha256.update((const uint8_t*)serviceId, strlen(serviceId));
  sha256.update((const uint8_t*)POLICY_ID, strlen(POLICY_ID));
  sha256.update((uint8_t*)&version, sizeof(version));
  sha256.update((const uint8_t*)creationNonce, strlen(creationNonce));

  sha256.finalize(hash, sizeof(hash));

  String password = "";

  for (int i = 0; i < PASSWORD_LENGTH; i++) {
    int index = hash[i % 32] % CHARSET_SIZE;
    password += PASSWORD_CHARS[index];
  }

  memset(hash, 0, sizeof(hash));

  return password;
}

String getArg(const String& input, int index) {
  int found = 0;
  int start = 0;
  int length = input.length();

  while (start < length && isCommandSeparator(input.charAt(start))) {
    start++;
  }

  for (int i = start; i <= length; i++) {
    if (i == length || isCommandSeparator(input.charAt(i))) {
      if (found == index) return input.substring(start, i);

      found++;

      start = i + 1;
      while (start < length && isCommandSeparator(input.charAt(start))) {
        start++;
      }
      i = start - 1;
    }
  }

  return "";
}

bool isCommandSeparator(char c) {
  return c == ' ' || c == '\t';
}
