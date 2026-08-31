/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import { DiagramService, STNode } from "@wso2/syntax-tree/lib/src";
import { PromptObject } from "./interfaces/mi-copilot";
import { Diagnostic } from "vscode-languageserver-types";
import { NotificationType, RequestType } from "vscode-messenger-common";

export enum ColorThemeKind {
    Light = 1,
    Dark = 2,
    HighContrast = 3,
    HighContrastLight = 4
}

export enum MACHINE_VIEW {
    Welcome = "Welcome to MI",
    ADD_ARTIFACT = "Add Artifact",
    Overview = "Project Overview",
    UnsupportedProject = "Unsupported Project",
    UnsupportedWorkspace = "Unsupported Workspace",
    Disabled = "MI Extension",
    Diagram = "MI Diagram",
    ResourceView = "Resource View",
    SequenceView = "Sequence View",
    SequenceTemplateView = "Sequence Template View",
    ProxyView = "Proxy View",
    DataServiceView = "DataService View",
    ServiceDesigner = "Service Designer",
    DataMapperView = "Data Mapper View",
    APIForm = "API Form",
    EndPointForm = "Endpoint Form",
    LoadBalanceEndPointForm = "Load Balance Endpoint Form",
    FailoverEndPointForm = "Failover Endpoint Form",
    RecipientEndPointForm = "Recipient Endpoint Form",
    TemplateEndPointForm = "Template Endpoint Form",
    SequenceForm = "Sequence Form",
    DatamapperForm = "Datamapper Form",
    InboundEPForm = "Event Integration Form",
    InboundEPView = "Event Integration View",
    MessageProcessorForm = "Message Processor Form",
    ProxyServiceForm = "Proxy Service Form",
    TaskForm = "Task Form",
    TaskView = "Task View",
    TemplateForm = "Template Form",
    HttpEndpointForm = "Http Endpoint Form",
    AddressEndpointForm = "Address Endpoint Form",
    WsdlEndpointForm = "Wsdl Endpoint Form",
    DefaultEndpointForm = "Default Endpoint Form",
    DataServiceForm = "Data Service Form",
    DssDataSourceForm = "DSS Data Source Form",
    DSSResourceServiceDesigner = "DSS Resource Designer",
    DSSQueryServiceDesigner = "DSS Query Designer",
    MCPServerForm = "MCPServerForm",
    MCPServerFromAPIsForm = "MCPServerFromAPIsForm",
    ProjectCreationForm = "Project Creation Form",
    ConvertToConsolidatedForm = "Convert To Consolidated Form",
    LocalEntryForm = "Local Entry Form",
    RegistryResourceForm = "Resource Creation Form",
    RegistryMetadataForm = "Registry Metadata Form",
    MessageStoreForm = "Message Store Form",
    ClassMediatorForm = "ClassMediator Creation Form",
    BallerinaModuleForm = "Ballerina Module Creation Form",
    DataSourceForm = "Data Source Creation Form",
    ImportArtifactForm = "Add Artifact Form",
    KubernetesConfigurationForm = "Configure Kubernetes Deployment Form",
    RegistryForm = "Manage Registry Properties Form",
    AddDriverPopup = "Add Driver Popup",
    Samples = "Samples",
    ImportProject = "Import Project",
    ConnectorStore = "Connector Store Form",
    ConnectionForm = "Connection Creation Form",
    TestSuite = "Test Suite Form",
    TestCase = "Test Case Form",
    AITestGen = "AI Test Generation",
    MockService = "Mock Service",
    LoggedOut = "Logged Out",
    UpdateExtension = "Update Extension",
    ManageDependencies = "Manage Dependencies",
    ManageConfigurables = "Manage Configurables",
    ProjectInformationForm = "Project Information Form",
    SETUP_ENVIRONMENT = "Setup Environment",
    ImportConnectorForm = "Import Connector",
    ImportInboundConnectorForm = "Import Inbound Endpoint",
    IdpConnectorSchemaGeneratorForm = "IDP Connector Schema Generator Form",
    DataMapperMigrationForm = "DataMapper Migration Form",
}

export enum AI_MACHINE_VIEW {
    AIOverview = "AI Overview",
    AIArtifact = "AI Artifact",
    AIChat = "AI Chat",
}

export type MachineStateValue =
    | 'initialize' | 'projectDetected' | 'oldProjectDetected' | 'oldWorkspaceDetected' | 'LSInit' | 'ready' | 'disabled'
    | { ready: 'viewReady' } | { ready: 'viewEditing' } | { ready: 'resolveMissingDependencies' }
    | { newProject: 'viewReady' }| { environmentSetup: 'viewReady' };

export type AIMachineStateValue =
    | 'Initialize'          // (checking auth, first load)
    | 'Unauthenticated'     // (show login window)
    | { Authenticating: 'determineFlow' | 'ssoFlow' | 'apiKeyFlow' | 'validatingApiKey' | 'awsBedrockFlow' | 'validatingAwsCredentials' } // hierarchical substates
    | 'Authenticated'       // (ready, main view)
    | 'UsageExceeded'       // (free usage quota exceeded, prompt user to set API key)
    | 'Disabled'            // (optional: if AI Chat is globally unavailable)
    | 'NotSupported';       // (workspace not supported)

export type PopupMachineStateValue = 'initialize' | 'ready' | { open: 'active' } | { ready: 'reopen' } | { ready: 'notify' } | 'disabled';

export type MiServerRunStatus = 'Running' | 'Stopped';

export enum AI_EVENT_TYPE {
    CHECK_AUTH = 'CHECK_AUTH',
    LOGIN = "LOGIN",
    AUTH_WITH_API_KEY = 'AUTH_WITH_API_KEY',
    SUBMIT_API_KEY = 'SUBMIT_API_KEY',
    AUTH_WITH_AWS_BEDROCK = 'AUTH_WITH_AWS_BEDROCK',
    SUBMIT_AWS_CREDENTIALS = 'SUBMIT_AWS_CREDENTIALS',
    SIGN_IN_SUCCESS = "SIGN_IN_SUCCESS",
    LOGOUT = "LOGOUT",
    SILENT_LOGOUT = "SILENT_LOGOUT",
    EXECUTE = "EXECUTE",
    CLEAR = "CLEAR",
    CLEAR_PROMPT = "CLEAR_PROMPT",
    DISPOSE = "DISPOSE",
    COMPLETE_AUTH = 'COMPLETE_AUTH',
    CANCEL = "CANCEL",
    CANCEL_LOGIN = 'CANCEL_LOGIN',
    RETRY = "RETRY",
    USAGE_EXCEEDED = "USAGE_EXCEEDED",
    USAGE_RESET = "USAGE_RESET",
    UPDATE_USAGE = "UPDATE_USAGE",
}

export type AIMachineEventMap = {
    [AI_EVENT_TYPE.CHECK_AUTH]: undefined;
    [AI_EVENT_TYPE.LOGIN]: undefined;
    [AI_EVENT_TYPE.AUTH_WITH_API_KEY]: undefined;
    [AI_EVENT_TYPE.SUBMIT_API_KEY]: { apiKey: string };
    [AI_EVENT_TYPE.AUTH_WITH_AWS_BEDROCK]: undefined;
    [AI_EVENT_TYPE.SUBMIT_AWS_CREDENTIALS]: {
        authType?: 'iam';
        accessKeyId?: string;
        secretAccessKey?: string;
        region: string;
        sessionToken?: string;
        tavilyApiKey?: string;
    } | {
        authType: 'api_key';
        apiKey: string;
        region: string;
        tavilyApiKey?: string;
    };
    [AI_EVENT_TYPE.SIGN_IN_SUCCESS]: undefined;
    [AI_EVENT_TYPE.LOGOUT]: undefined;
    [AI_EVENT_TYPE.SILENT_LOGOUT]: undefined;
    [AI_EVENT_TYPE.EXECUTE]: undefined;
    [AI_EVENT_TYPE.CLEAR]: undefined;
    [AI_EVENT_TYPE.CLEAR_PROMPT]: undefined;
    [AI_EVENT_TYPE.DISPOSE]: undefined;
    [AI_EVENT_TYPE.COMPLETE_AUTH]: undefined;
    [AI_EVENT_TYPE.CANCEL]: undefined;
    [AI_EVENT_TYPE.CANCEL_LOGIN]: undefined;
    [AI_EVENT_TYPE.RETRY]: undefined;
    [AI_EVENT_TYPE.USAGE_EXCEEDED]: undefined;
    [AI_EVENT_TYPE.USAGE_RESET]: undefined;
    [AI_EVENT_TYPE.UPDATE_USAGE]: { usage: any };
};

export type AIMachineSendableEvent =
    | { [K in keyof AIMachineEventMap]: AIMachineEventMap[K] extends undefined
        ? { type: K }
        : { type: K; payload: AIMachineEventMap[K] }
    }[keyof AIMachineEventMap];

export enum LoginMethod {
    MI_INTEL = 'miIntel',
    ANTHROPIC_KEY = 'anthropic_key',
    AWS_BEDROCK = 'aws_bedrock'
}

interface MIIntelSecrets {
    accessToken: string;
    refreshToken?: string;
    expiresAt?: number;
}

interface AnthropicKeySecrets {
    apiKey: string;
}

export type AwsBedrockAuthType = 'iam' | 'api_key';

/** Logical model slots that can be pointed at an Application Inference Profile. */
export type BedrockInferenceProfileSlot = 'haiku' | 'sonnet' | 'opus';

/**
 * Optional Bedrock Application Inference Profile ARNs, one per logical model.
 *
 * Some organizations attach an SCP/IAM policy that denies direct invocation of
 * `foundation-model/*` and system-defined `inference-profile/*` resources, and
 * permits `bedrock:InvokeModel` only against customer-owned, taggable
 * application inference profiles — the standard cost-allocation / chargeback
 * pattern. Without an override, both sign-in and every subsequent Copilot
 * request are denied in such accounts.
 *
 * Any slot left unset falls back to the built-in `global.` system profile, so
 * existing users are unaffected.
 */
export type BedrockInferenceProfileOverrides = Partial<Record<BedrockInferenceProfileSlot, string>>;

/** Shape of a Bedrock application inference profile ARN. */
const BEDROCK_APP_INFERENCE_PROFILE_ARN =
    /^arn:aws(?:-us-gov|-cn)?:bedrock:[a-z0-9-]+:\d{12}:application-inference-profile\/[A-Za-z0-9._-]+$/;

/** True when `value` looks like a Bedrock application inference profile ARN. */
export const isBedrockApplicationInferenceProfileArn = (value: string): boolean =>
    BEDROCK_APP_INFERENCE_PROFILE_ARN.test(value.trim());

/**
 * Extract the region embedded in an application inference profile ARN, or
 * undefined when the value is not a well-formed ARN. Used to warn when the ARN
 * disagrees with the region the user entered.
 */
export const getRegionFromInferenceProfileArn = (value: string): string | undefined => {
    if (!isBedrockApplicationInferenceProfileArn(value)) {
        return undefined;
    }
    return value.trim().split(':')[3];
};

/** Every override slot, in the order they are presented to the user. */
export const BEDROCK_INFERENCE_PROFILE_SLOTS: BedrockInferenceProfileSlot[] = ['haiku', 'sonnet', 'opus'];

/**
 * Slots the default model presets resolve to, so a partial configuration cannot
 * pass sign-in and then fail on the first request. Sonnet 4.6 is the default
 * main model and Haiku 4.5 the default sub-agent model; Opus is opt-in, so a
 * profile for it is only needed if the user selects it in Settings.
 */
export const REQUIRED_BEDROCK_INFERENCE_PROFILE_SLOTS: BedrockInferenceProfileSlot[] = ['haiku', 'sonnet'];

/** Trim and drop empty slots so an all-blank advanced form is stored as undefined. */
export const normalizeInferenceProfileOverrides = (
    overrides: BedrockInferenceProfileOverrides | undefined
): BedrockInferenceProfileOverrides | undefined => {
    if (!overrides) {
        return undefined;
    }

    const normalized: BedrockInferenceProfileOverrides = {};
    let count = 0;
    for (const slot of BEDROCK_INFERENCE_PROFILE_SLOTS) {
        const arn = overrides[slot]?.trim();
        if (arn) {
            normalized[slot] = arn;
            count++;
        }
    }
    return count ? normalized : undefined;
};

/**
 * Once any Application Inference Profile is configured, the account is one that
 * forbids the default `global.` profiles — so every slot the default presets use
 * must be supplied too. Returns the missing slots (empty when nothing is set, or
 * when the configuration is complete).
 */
export const findMissingRequiredInferenceProfiles = (
    overrides: BedrockInferenceProfileOverrides | undefined
): BedrockInferenceProfileSlot[] => {
    const normalized = normalizeInferenceProfileOverrides(overrides);
    if (!normalized) {
        return [];
    }
    return REQUIRED_BEDROCK_INFERENCE_PROFILE_SLOTS.filter((slot) => !normalized[slot]);
};

export interface AwsBedrockIamSecrets {
    authType?: 'iam';
    accessKeyId: string;
    secretAccessKey: string;
    region: string;
    sessionToken?: string;
    /** Optional Tavily API key for web search/fetch on Bedrock (Bedrock has no first-party web tools). */
    tavilyApiKey?: string;
    /** Optional Application Inference Profile ARNs; see BedrockInferenceProfileOverrides. */
    inferenceProfiles?: BedrockInferenceProfileOverrides;
}

export interface AwsBedrockApiKeySecrets {
    authType: 'api_key';
    apiKey: string;
    region: string;
    /** Optional Tavily API key for web search/fetch on Bedrock (Bedrock has no first-party web tools). */
    tavilyApiKey?: string;
    /** Optional Application Inference Profile ARNs; see BedrockInferenceProfileOverrides. */
    inferenceProfiles?: BedrockInferenceProfileOverrides;
}

export type AwsBedrockSecrets = AwsBedrockIamSecrets | AwsBedrockApiKeySecrets;

export type AuthCredentials =
    | {
        loginMethod: LoginMethod.MI_INTEL;
        secrets: MIIntelSecrets;
    }
    | {
        loginMethod: LoginMethod.ANTHROPIC_KEY;
        secrets: AnthropicKeySecrets;
    }
    | {
        loginMethod: LoginMethod.AWS_BEDROCK;
        secrets: AwsBedrockSecrets;
    };

export interface AIUserToken {
    token: string; // For MI Intel, this is the access token and for Anthropic, this is the API key
}

export interface AIMachineContext {
    loginMethod?: LoginMethod;
    userToken?: AIUserToken;
    usage?: AIUserTokens;
    errorMessage?: string;
}

export enum EVENT_TYPE {
    OPEN_VIEW = "OPEN_VIEW",
    REPLACE_VIEW = "REPLACE_VIEW",
    CLEAR_PROMPT = "CLEAR_PROMPT",
    REFRESH_ENVIRONMENT = "REFRESH_ENVIRONMENT",
}

export enum POPUP_EVENT_TYPE {
    OPEN_VIEW = "OPEN_VIEW",
    CLOSE_VIEW = "CLOSE_VIEW"
}

export enum Platform {
    WINDOWS,
    MAC,
    LINUX
}

export type VoidCommands = "OPEN_LOW_CODE" | "OPEN_PROJECT" | "CREATE_PROJECT";

export interface MachineEvent {
    type: EVENT_TYPE;
}

export interface CommandProps {
    command: VoidCommands;
    projectName?: string;
    isService?: boolean
}

export interface ErrorType {
    title: string;
    message: string;
}

interface DataMapperProps {
    filePath: string;
    functionName?: string;
    fileContent?: string;
    nonMappingFileContent?: string;
    configName: string;
}

// State Machine context values
export interface VisualizerLocation {
    view: MACHINE_VIEW | null;
    stNode?: STNode | DiagramService;
    diagnostics?: Diagnostic[]
    errors?: ErrorType[];
    documentUri?: string;
    projectUri?: string;
    platform?: Platform;
    pathSeparator?: string;
    identifier?: string;
    position?: any;
    projectOpened?: boolean;
    isOldProject?: boolean;
    displayOverview?: boolean;
    customProps?: any;
    dataMapperProps?: DataMapperProps;
    type?: string;
    connectorData?: any[];
    previousContext?: any;
    env?: { [key: string]: string | undefined };
    isLoading?: boolean;
    isLegacyRuntime?: boolean;
}

export interface PopupVisualizerLocation extends VisualizerLocation {
    recentIdentifier?: string;
}

export interface AIVisualizerLocation {
    view?: AI_MACHINE_VIEW | null;
    initialPrompt?: PromptObject;
    state?: AIMachineStateValue;
    loginMethod?: LoginMethod;
    userToken?: AIUserToken;
    usage?: AIUserTokens;
    errorMessage?: string;
}

export interface AIUserTokens {
    remainingUsagePercentage?: number;
    resetsIn?: number;
}

export interface ParentPopupData {
    recentIdentifier: string;
}

export interface SwaggerData {
    generatedSwagger: any;
    port: number;
}

export interface ConnectorStatus {
    connector: string;
    isSuccess: boolean;
    message: string;
}

export interface DownloadProgressData {
    percentage: number;
    downloadedAmount: string;
    downloadSize: string;
}

export interface Document {
    uri: string;
}

export const stateChanged: NotificationType<MachineStateValue> = { method: 'stateChanged' };
export const aiStateChanged: NotificationType<AIMachineStateValue> = { method: 'aiStateChanged' };
export const popupStateChanged: NotificationType<PopupMachineStateValue> = { method: 'popupStateChanged' };
export const themeChanged: NotificationType<ColorThemeKind> = { method: 'themeChanged' };
export const getVisualizerState: RequestType<void, VisualizerLocation> = { method: 'getVisualizerState' };
export const getAIVisualizerState: RequestType<void, AIVisualizerLocation> = { method: 'getAIVisualizerState' };
export const getPopupVisualizerState: RequestType<void, PopupVisualizerLocation> = { method: 'getPopupVisualizerState' };
export const sendAIStateEvent: RequestType<AI_EVENT_TYPE, void> = { method: 'sendAIStateEvent' };
export const onFileContentUpdate: NotificationType<void> = { method: `onFileContentUpdate` };
export const webviewReady: NotificationType<void> = { method: `webviewReady` };
export const onSwaggerSpecReceived: NotificationType<SwaggerData> = { method: `onSwaggerSpecReceived` };
export const miServerRunStateChanged: NotificationType<MiServerRunStatus> = { method: `miServerRunStateChanged` };
export const onParentPopupSubmitted: NotificationType<ParentPopupData> = { method: `onParentPopupSubmitted` };
export const onConnectorStatusUpdate: NotificationType<ConnectorStatus> = { method: `onConnectorStatusUpdate` };
export const onDownloadProgress: NotificationType<DownloadProgressData> = { method: `onDownloadProgress` };
export const onDocumentSave: NotificationType<Document> = { method: `onDocumentSave` };
