/**
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com/) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import * as assert from 'assert';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import {
    isClassMediatorPath,
    getExpectedSynapseCoreVersion,
    checkClassMediatorPomStatus,
    buildClassMediatorPomReminder,
    SYNAPSE_CORE_VERSION_GE_460,
    SYNAPSE_CORE_VERSION_LT_460,
} from '../../ai-features/agent-mode/tools/file_tools';

suite('Class Mediator POM Reminder Tests', () => {
    test('isClassMediatorPath correctly identifies class mediator Java paths', () => {
        assert.strictEqual(isClassMediatorPath('src/main/java/org/wso2/sample/MyMediator.java'), true);
        assert.strictEqual(isClassMediatorPath('src\\main\\java\\org\\wso2\\sample\\MyMediator.java'), true);
        assert.strictEqual(isClassMediatorPath('/home/user/project/src/main/java/org/wso2/sample/MyMediator.java'), true);
        assert.strictEqual(isClassMediatorPath('src/main/wso2mi/artifacts/apis/MyApi.xml'), false);
        assert.strictEqual(isClassMediatorPath('src/test/java/org/wso2/sample/MyMediatorTest.java'), false);
        assert.strictEqual(isClassMediatorPath('src/main/java/README.md'), false);
    });

    test('getExpectedSynapseCoreVersion returns correct version for MI runtime', () => {
        assert.strictEqual(getExpectedSynapseCoreVersion('4.6.0'), SYNAPSE_CORE_VERSION_GE_460);
        assert.strictEqual(getExpectedSynapseCoreVersion('4.6.1'), SYNAPSE_CORE_VERSION_GE_460);
        assert.strictEqual(getExpectedSynapseCoreVersion('5.0.0'), SYNAPSE_CORE_VERSION_GE_460);
        assert.strictEqual(getExpectedSynapseCoreVersion(null), SYNAPSE_CORE_VERSION_GE_460);
        assert.strictEqual(getExpectedSynapseCoreVersion(undefined), SYNAPSE_CORE_VERSION_GE_460);

        assert.strictEqual(getExpectedSynapseCoreVersion('4.5.0'), SYNAPSE_CORE_VERSION_LT_460);
        assert.strictEqual(getExpectedSynapseCoreVersion('4.4.0'), SYNAPSE_CORE_VERSION_LT_460);
        assert.strictEqual(getExpectedSynapseCoreVersion('4.3.0'), SYNAPSE_CORE_VERSION_LT_460);
        assert.strictEqual(getExpectedSynapseCoreVersion('4.2.0'), SYNAPSE_CORE_VERSION_LT_460);
        assert.strictEqual(getExpectedSynapseCoreVersion('4.1.0'), SYNAPSE_CORE_VERSION_LT_460);
    });

    test('checkClassMediatorPomStatus identifies properly configured POM for 4.6.0+', () => {
        const pomContent = `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>sample-project</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    <properties>
        <project.runtime.version>4.6.0</project.runtime.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.apache.synapse</groupId>
            <artifactId>synapse-core</artifactId>
            <version>4.1.0-wso2v48</version>
        </dependency>
    </dependencies>
</project>`;

        const status = checkClassMediatorPomStatus(pomContent);
        assert.strictEqual(status.isJarPackaging, true);
        assert.strictEqual(status.runtimeVersion, '4.6.0');
        assert.strictEqual(status.expectedSynapseCoreVersion, '4.1.0-wso2v48');
        assert.strictEqual(status.hasSynapseCore, true);
        assert.strictEqual(status.currentSynapseCoreVersion, '4.1.0-wso2v48');
        assert.strictEqual(status.isSynapseCoreVersionCorrect, true);
        assert.strictEqual(status.isConfigured, true);
    });

    test('checkClassMediatorPomStatus identifies properly configured POM for < 4.6.0', () => {
        const pomContent = `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>sample-project</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    <properties>
        <project.runtime.version>4.4.0</project.runtime.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.apache.synapse</groupId>
            <artifactId>synapse-core</artifactId>
            <version>4.0.0-wso2v165</version>
        </dependency>
    </dependencies>
</project>`;

        const status = checkClassMediatorPomStatus(pomContent);
        assert.strictEqual(status.isJarPackaging, true);
        assert.strictEqual(status.runtimeVersion, '4.4.0');
        assert.strictEqual(status.expectedSynapseCoreVersion, '4.0.0-wso2v165');
        assert.strictEqual(status.hasSynapseCore, true);
        assert.strictEqual(status.currentSynapseCoreVersion, '4.0.0-wso2v165');
        assert.strictEqual(status.isSynapseCoreVersionCorrect, true);
        assert.strictEqual(status.isConfigured, true);
    });

    test('checkClassMediatorPomStatus detects packaging:pom and missing dependency', () => {
        const pomContent = `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>sample-project</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    <properties>
        <project.runtime.version>4.6.0</project.runtime.version>
    </properties>
    <dependencies>
    </dependencies>
</project>`;

        const status = checkClassMediatorPomStatus(pomContent);
        assert.strictEqual(status.isJarPackaging, false);
        assert.strictEqual(status.hasSynapseCore, false);
        assert.strictEqual(status.isConfigured, false);
    });

    test('checkClassMediatorPomStatus detects mismatched synapse-core version', () => {
        const pomContent = `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>sample-project</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    <properties>
        <project.runtime.version>4.6.0</project.runtime.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.apache.synapse</groupId>
            <artifactId>synapse-core</artifactId>
            <version>4.0.0-wso2v165</version>
        </dependency>
    </dependencies>
</project>`;

        const status = checkClassMediatorPomStatus(pomContent);
        assert.strictEqual(status.isJarPackaging, true);
        assert.strictEqual(status.hasSynapseCore, true);
        assert.strictEqual(status.isSynapseCoreVersionCorrect, false);
        assert.strictEqual(status.isConfigured, false);
    });

    test('checkClassMediatorPomStatus ignores XML comments properly', () => {
        const pomContent = `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>sample-project</artifactId>
    <version>1.0.0</version>
    <!-- <packaging>jar</packaging> -->
    <packaging>pom</packaging>
    <properties>
        <project.runtime.version>4.6.0</project.runtime.version>
    </properties>
    <dependencies>
        <!--
        <dependency>
            <groupId>org.apache.synapse</groupId>
            <artifactId>synapse-core</artifactId>
            <version>4.1.0-wso2v48</version>
        </dependency>
        -->
    </dependencies>
</project>`;

        const status = checkClassMediatorPomStatus(pomContent);
        assert.strictEqual(status.isJarPackaging, false);
        assert.strictEqual(status.hasSynapseCore, false);
        assert.strictEqual(status.isConfigured, false);
    });

    test('checkClassMediatorPomStatus handles missing <version> tag in synapse-core dependency', () => {
        const pomContent = `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>sample-project</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    <properties>
        <project.runtime.version>4.6.0</project.runtime.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.apache.synapse</groupId>
            <artifactId>synapse-core</artifactId>
        </dependency>
    </dependencies>
</project>`;

        const status = checkClassMediatorPomStatus(pomContent);
        assert.strictEqual(status.hasSynapseCore, true);
        assert.strictEqual(status.currentSynapseCoreVersion, undefined);
        assert.strictEqual(status.isSynapseCoreVersionCorrect, false);
        assert.strictEqual(status.isConfigured, false);
    });

    test('buildClassMediatorPomReminder returns empty string when fully configured', () => {
        const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'mi-test-cm-'));
        try {
            const pomContent = `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>sample-project</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    <properties>
        <project.runtime.version>4.6.0</project.runtime.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.apache.synapse</groupId>
            <artifactId>synapse-core</artifactId>
            <version>4.1.0-wso2v48</version>
        </dependency>
    </dependencies>
</project>`;
            fs.writeFileSync(path.join(tempDir, 'pom.xml'), pomContent, 'utf-8');
            const reminder = buildClassMediatorPomReminder(tempDir);
            assert.strictEqual(reminder, '');
        } finally {
            fs.rmSync(tempDir, { recursive: true, force: true });
        }
    });

    test('buildClassMediatorPomReminder generates reminder with 4.1.0-wso2v48 for 4.6.0 runtime', () => {
        const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'mi-test-cm-'));
        try {
            const pomContent = `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>sample-project</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    <properties>
        <project.runtime.version>4.6.0</project.runtime.version>
    </properties>
</project>`;
            fs.writeFileSync(path.join(tempDir, 'pom.xml'), pomContent, 'utf-8');
            const reminder = buildClassMediatorPomReminder(tempDir);
            assert.ok(reminder.includes('<system-reminder>'));
            assert.ok(reminder.includes('<packaging>jar</packaging>'));
            assert.ok(reminder.includes('4.1.0-wso2v48'));
            assert.ok(reminder.includes('org.apache.synapse'));
            assert.ok(reminder.includes('synapse-core'));
        } finally {
            fs.rmSync(tempDir, { recursive: true, force: true });
        }
    });

    test('buildClassMediatorPomReminder generates reminder with 4.0.0-wso2v165 for < 4.6.0 runtime', () => {
        const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'mi-test-cm-'));
        try {
            const pomContent = `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>sample-project</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    <properties>
        <project.runtime.version>4.4.0</project.runtime.version>
    </properties>
</project>`;
            fs.writeFileSync(path.join(tempDir, 'pom.xml'), pomContent, 'utf-8');
            const reminder = buildClassMediatorPomReminder(tempDir);
            assert.ok(reminder.includes('<system-reminder>'));
            assert.ok(reminder.includes('<packaging>jar</packaging>'));
            assert.ok(reminder.includes('4.0.0-wso2v165'));
            assert.ok(reminder.includes('org.apache.synapse'));
            assert.ok(reminder.includes('synapse-core'));
        } finally {
            fs.rmSync(tempDir, { recursive: true, force: true });
        }
    });

    test('buildClassMediatorPomReminder generates fallback reminder when pom.xml is missing', () => {
        const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'mi-test-cm-'));
        try {
            const reminder = buildClassMediatorPomReminder(tempDir);
            assert.ok(reminder.includes('<system-reminder>'));
            assert.ok(reminder.includes('could not be read'));
            assert.ok(reminder.includes('<packaging>jar</packaging>'));
            assert.ok(reminder.includes('org.apache.synapse:synapse-core'));
        } finally {
            fs.rmSync(tempDir, { recursive: true, force: true });
        }
    });
});
