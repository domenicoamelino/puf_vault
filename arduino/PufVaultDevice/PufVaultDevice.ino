#include <EEPROM.h>
#include <SHA256.h>

#define BAUD_RATE 115200

#define MAX_SLOTS 6
#define PUF_SIZE_BYTES 64
#define PASSWORD_LENGTH 16
#define EEPROM_MAGIC 0x51

const char USER_ID[] = "user001";
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
  char serviceId[32];
  uint32_t version;
};

struct DeviceStorage {
  uint8_t magic;
  ServiceSlot slots[MAX_SLOTS];
};

DeviceStorage storage;

bool powerCycleRequired = false;

void setup() {
  Serial.begin(BAUD_RATE);

  delay(500);

  capturePuf();
  loadStorage();

  Serial.println("PUFVAULT_ARDUINO_READY");
}

void loop() {
  if (Serial.available() <= 0) {
    return;
  }

  String command = Serial.readStringUntil('\n');
  command.trim();

  if (command.length() == 0) {
    return;
  }

  handleCommand(command);
}

void handleCommand(String command) {
  String op = getArg(command, 0);

  if (op == "PING") {
    Serial.println("OK PONG");
    return;
  }

  if (op == "STATUS") {
    if (powerCycleRequired) {
      Serial.println("NOK POWER_CYCLE_REQUIRED");
    } else {
      Serial.println("OK READY");
    }
    return;
  }

  if (op == "CAPABILITY") {
    Serial.print("OK USER=");
    Serial.print(USER_ID);
    Serial.print(" SLOTS=");
    Serial.print(MAX_SLOTS);
    Serial.print(" POLICY=");
    Serial.println(POLICY_ID);
    return;
  }

  if (op == "LIST_SERVICES") {
    Serial.println("SERVICES_BEGIN");

    for (int i = 0; i < MAX_SLOTS; i++) {
      Serial.print("SLOT ");
      Serial.print(i);
      Serial.print(" ");

      if (storage.slots[i].active) {
        Serial.print("ACTIVE ");
        Serial.print(storage.slots[i].serviceId);
        Serial.print(" VERSION ");
        Serial.println(storage.slots[i].version);
      } else {
        Serial.println("FREE");
      }
    }

    Serial.println("SERVICES_END");
    return;
  }

  if (powerCycleRequired) {
    Serial.println("NOK POWER_CYCLE_REQUIRED");
    return;
  }

  if (op == "ADD_SERVICE") {
    String serviceId = getArg(command, 1);

    if (!isValidServiceId(serviceId)) {
      Serial.println("NOK INVALID_SERVICE_ID");
      return;
    }

    if (findService(serviceId.c_str()) >= 0) {
      Serial.println("NOK SERVICE_EXISTS");
      return;
    }

    int freeSlot = findFreeSlot();

    if (freeSlot < 0) {
      Serial.println("NOK NO_FREE_SLOT");
      return;
    }

    storage.slots[freeSlot].active = true;
    storage.slots[freeSlot].version = 0;

    memset(storage.slots[freeSlot].serviceId, 0, sizeof(storage.slots[freeSlot].serviceId));

    strncpy(
      storage.slots[freeSlot].serviceId,
      serviceId.c_str(),
      sizeof(storage.slots[freeSlot].serviceId) - 1
    );

    saveStorage();

    Serial.print("OK SERVICE_ADDED SLOT=");
    Serial.println(freeSlot);
    return;
  }

  if (op == "DELETE_SERVICE") {
    String serviceId = getArg(command, 1);

    int index = findService(serviceId.c_str());

    if (index < 0) {
      Serial.println("NOK SERVICE_NOT_FOUND");
      return;
    }

    memset(&storage.slots[index], 0, sizeof(ServiceSlot));
    saveStorage();

    powerCycleRequired = true;

    Serial.println("OK SERVICE_DELETED POWER_CYCLE_REQUIRED");
    return;
  }

  if (op == "GENERATE_PASSWORD") {
    String serviceId = getArg(command, 1);

    int index = findService(serviceId.c_str());

    if (index < 0) {
      Serial.println("NOK SERVICE_NOT_FOUND");
      return;
    }

    String password = generatePassword(
      storage.slots[index].serviceId,
      storage.slots[index].version
    );

    Serial.print("OK PASSWORD ");
    Serial.println(password);
    return;
  }

  if (op == "ROTATE_SERVICE") {
    String serviceId = getArg(command, 1);

    int index = findService(serviceId.c_str());

    if (index < 0) {
      Serial.println("NOK SERVICE_NOT_FOUND");
      return;
    }

    storage.slots[index].version++;
    saveStorage();

    Serial.print("OK SERVICE_ROTATED VERSION=");
    Serial.println(storage.slots[index].version);
    return;
  }

  if (op == "WIPE_ALL") {
    memset(&storage, 0, sizeof(storage));
    storage.magic = EEPROM_MAGIC;
    saveStorage();

    powerCycleRequired = true;

    Serial.println("OK WIPED POWER_CYCLE_REQUIRED");
    return;
  }

  Serial.println("NOK UNKNOWN_COMMAND");
}

void capturePuf() {
  volatile uint8_t marker = 0;
  uint8_t* sram = (uint8_t*)0x0100;

  for (int i = 0; i < PUF_SIZE_BYTES; i++) {
    puf_buffer[i] = sram[i] ^ marker;
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

int findFreeSlot() {
  for (int i = 0; i < MAX_SLOTS; i++) {
    if (!storage.slots[i].active) {
      return i;
    }
  }

  return -1;
}

int findService(const char* serviceId) {
  for (int i = 0; i < MAX_SLOTS; i++) {
    if (!storage.slots[i].active) {
      continue;
    }

    if (strcmp(storage.slots[i].serviceId, serviceId) == 0) {
      return i;
    }
  }

  return -1;
}

bool isValidServiceId(String serviceId) {
  if (serviceId.length() == 0) {
    return false;
  }

  if (serviceId.length() >= 31) {
    return false;
  }

  for (int i = 0; i < serviceId.length(); i++) {
    char c = serviceId.charAt(i);

    bool valid =
      (c >= 'A' && c <= 'Z') ||
      (c >= 'a' && c <= 'z') ||
      (c >= '0' && c <= '9') ||
      c == '.' ||
      c == '-' ||
      c == '_';

    if (!valid) {
      return false;
    }
  }

  return true;
}

String generatePassword(const char* serviceId, uint32_t version) {
  uint8_t hash[32];

  sha256.reset();

  sha256.update(puf_buffer, PUF_SIZE_BYTES);
  sha256.update((const uint8_t*)USER_ID, strlen(USER_ID));
  sha256.update((const uint8_t*)serviceId, strlen(serviceId));
  sha256.update((const uint8_t*)POLICY_ID, strlen(POLICY_ID));
  sha256.update((uint8_t*)&version, sizeof(version));

  sha256.finalize(hash, sizeof(hash));

  String password = "";

  for (int i = 0; i < PASSWORD_LENGTH; i++) {
    int index = hash[i % 32] % CHARSET_SIZE;
    password += PASSWORD_CHARS[index];
  }

  memset(hash, 0, sizeof(hash));

  return password;
}

String getArg(String input, int index) {
  int found = 0;
  int start = 0;

  for (int i = 0; i <= input.length(); i++) {
    if (i == input.length() || input.charAt(i) == ' ') {
      if (found == index) {
        return input.substring(start, i);
      }

      found++;
      start = i + 1;
    }
  }

  return "";
}