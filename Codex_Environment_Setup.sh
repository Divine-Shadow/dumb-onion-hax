#!/usr/bin/env bash
set -euxo pipefail

###############################################################################
# [1] Minimal system deps
###############################################################################
apt-get update
apt-get install -y --no-install-recommends curl ca-certificates tar gzip

###############################################################################
# [2] Temurin 21 JDK
###############################################################################
JDK_API_URL="https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse"
install -d /opt/jdk
curl -fsSL --retry 3 --retry-connrefused --retry-delay 2 -o /tmp/jdk.tgz "$JDK_API_URL"
tar -xzf /tmp/jdk.tgz --strip-components=1 -C /opt/jdk
rm /tmp/jdk.tgz

export JAVA_HOME=/opt/jdk
export PATH="$JAVA_HOME/bin:$PATH"

###############################################################################
# [2b] Import system certificates into the vendor JDK
###############################################################################
echo ">> Importing system certificates into JDK cacerts..."
STOREPASS=changeit
CERT_DIR=/etc/ssl/certs
for crt in "$CERT_DIR"/*.pem "$CERT_DIR"/*.crt; do
  [ -e "$crt" ] || continue
  alias=$(basename "$crt" | tr -cd '[:alnum:]_-')
  keytool -importcert -noprompt -trustcacerts \
          -alias "$alias" -file "$crt" \
          -keystore "$JAVA_HOME/lib/security/cacerts" \
          -storepass "$STOREPASS" 2>/dev/null || true
done
echo ">> System CAs imported."

###############################################################################
# Persist JAVA_HOME/PATH
###############################################################################
cat <<'EOF' >/etc/profile.d/java_sbt.sh
export JAVA_HOME=/opt/jdk
export PATH="/opt/jdk/bin:/opt/sbt/bin:$PATH"
EOF
chmod +x /etc/profile.d/java_sbt.sh

###############################################################################
# [3] sbt 1.9.0
###############################################################################
SBT_VERSION=1.9.0
SBT_TGZ_URL="https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz"
install -d /opt/sbt
curl -fsSL --retry 3 --retry-connrefused --retry-delay 2 -o /tmp/sbt.tgz "$SBT_TGZ_URL"
tar -xzf /tmp/sbt.tgz --strip-components=1 -C /opt/sbt
rm /tmp/sbt.tgz
export PATH="/opt/sbt/bin:$PATH"

###############################################################################
# [4] Smoke tests
###############################################################################
java  -version
sbt   --script-version

###############################################################################
# [5] Warm cache + bloop (if build present)
###############################################################################
if [[ -f build.sbt ]]; then
  sbt update compile bloopInstall
fi
