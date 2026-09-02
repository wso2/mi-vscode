/**
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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

import styled from "@emotion/styled";

export const CardButton = styled.button`
    width: 90px;
    height: 44px;
    box-sizing: border-box;
    padding: 0 24px;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 500;
    font-family: inherit;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    align-self: flex-start;
    transition: background-color 0.1s ease;

    &:focus-visible {
        outline: 1px solid var(--vscode-focusBorder);
        outline-offset: 2px;
    }
`;

export const PrimaryCardButton = styled(CardButton)`
    background-color: var(--vscode-button-background);
    color: var(--vscode-button-foreground);
    border: 1px solid var(--vscode-button-border, transparent);

    &:hover {
        background-color: var(--vscode-button-hoverBackground);
    }
`;

export const SecondaryCardButton = styled(CardButton)`
    background-color: var(--vscode-button-secondaryBackground);
    color: var(--vscode-button-secondaryForeground);
    border: 1px solid var(--vscode-button-border, var(--vscode-contrastBorder, transparent));

    &:hover {
        background-color: var(--vscode-button-secondaryHoverBackground);
    }
`;

export const Wrapper = styled.div`
    --wso2-brand-primary: #17223A;
    --wso2-brand-primary-alt: #1B2A49;
    --wso2-brand-primary-deep: #35537D;
    --wso2-brand-ink: #17223A;
    --wso2-brand-ink-alt: #1B2A49;
    --wso2-brand-ink-deep: #0B1220;
    --wso2-brand-hero-start: #0B1220;
    --wso2-brand-hero-end: #223150;
    --wso2-brand-accent: #5CD1FF;
    --wso2-brand-accent-alt: #3A90BF;
    --wso2-brand-accent-soft: #B7E4FC;
    --wso2-brand-neutral-900: #0D0D0D;
    --wso2-brand-neutral-700: #262626;
    --wso2-brand-neutral-600: #565656;
    --wso2-brand-neutral-300: #D9D9D9;
    --wso2-brand-neutral-100: #F2F2F2;
    --wso2-brand-white: #FFFFFF;
    --welcome-library-accent: #3AADA5;
    --welcome-project-accent: #C07D18;
    --welcome-import-accent: #7C5FB5;
    --welcome-open-project-accent: #4B7FA2;
    --welcome-hero-muted: color-mix(in srgb, var(--wso2-brand-white) 78%, transparent);

    width: 100%;
    height: 100vh;
    overflow-y: auto;
    font-family: var(--vscode-font-family);
    font-size: var(--vscode-font-size);
    color: var(--vscode-foreground);
    background-color: var(--vscode-editor-background);
`;

export const Hero = styled.div`
    background: linear-gradient(180deg, var(--wso2-brand-hero-start) 0%, var(--wso2-brand-hero-end) 100%);
    padding: 48px 60px 80px;
    width: 100%;
    box-sizing: border-box;
`;

export const Headline = styled.h1`
    font-size: 48px;
    font-weight: 700;
    margin: 0;
    color: var(--wso2-brand-white);
`;

export const Caption = styled.p`
    font-size: 16px;
    line-height: 1.6;
    margin: 16px 0 0 0;
    max-width: 800px;
    color: var(--welcome-hero-muted);
`;

export const CardsContainer = styled.div`
    padding: 0 60px 60px;
    margin: -40px auto 0;
    max-width: 1160px;
    box-sizing: border-box;
    position: relative;
    z-index: 1;
`;

export const CardsGrid = styled.div`
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 24px;

    @media (max-width: 640px) {
        grid-template-columns: minmax(0, 1fr);
    }
`;

export const ActionCard = styled.div`
    background: var(--vscode-editor-background);
    border-radius: 12px;
    padding: 28px 24px;
    display: flex;
    flex-direction: column;
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
    transition: transform 0.2s ease, box-shadow 0.2s ease;

    &:hover {
        transform: translateY(-4px);
        box-shadow: 0 8px 16px rgba(0, 0, 0, 0.25);
    }
`;

export const CardIcon = styled.div<{ bg: string }>`
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 20px;
    color: var(--wso2-brand-white);
    background: ${(props: { bg: string }) => props.bg};
`;

export const CardTitle = styled.h3`
    font-size: 20px;
    font-weight: 600;
    margin: 0 0 12px 0;
    color: var(--vscode-foreground);
`;

export const CardDescription = styled.p`
    font-size: 14px;
    line-height: 1.6;
    margin: 0 0 24px 0;
    color: var(--vscode-descriptionForeground);
    flex: 1;
`;

export const MoreToggleWrapper = styled.div`
    display: flex;
    align-items: center;
    gap: 16px;
    margin: 28px 0 20px;
`;

export const MoreDivider = styled.div`
    flex: 1;
    height: 1px;
    background: var(--vscode-widget-border, rgba(128, 128, 128, 0.2));
`;

export const MoreToggleButton = styled.button`
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 18px;
    background: transparent;
    border: 1px solid var(--vscode-widget-border, rgba(128, 128, 128, 0.3));
    border-radius: 20px;
    color: var(--vscode-descriptionForeground);
    font-size: 13px;
    cursor: pointer;
    white-space: nowrap;

    &:hover {
        background: var(--vscode-list-hoverBackground);
        color: var(--vscode-foreground);
    }
`;

export const SecondaryCardsSection = styled.div`
    overflow: hidden;
    transition: max-height 0.3s ease, opacity 0.2s ease;
`;

export const SecondaryActionRow = styled.button`
    width: 100%;
    background: transparent;
    border-radius: 10px;
    padding: 12px 16px;
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: 14px;
    border: 1px solid var(--vscode-widget-border, rgba(128, 128, 128, 0.2));
    color: inherit;
    font: inherit;
    text-align: left;
    cursor: pointer;

    &:hover {
        background: var(--vscode-list-hoverBackground);
    }

    &:focus-visible {
        outline: 1px solid var(--vscode-focusBorder);
        outline-offset: 2px;
    }
`;

export const SecondaryRowIcon = styled.div`
    width: 36px;
    height: 36px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--wso2-brand-primary-alt);
    color: var(--wso2-brand-white);
    flex-shrink: 0;
`;

export const SecondaryRowContent = styled.div`
    flex: 1;
    min-width: 0;
`;

export const SecondaryRowTitle = styled.span`
    display: block;
    font-size: 13px;
    font-weight: 600;
    color: var(--vscode-foreground);
    margin-bottom: 2px;
`;

export const SecondaryRowDescription = styled.span`
    display: block;
    font-size: 12px;
    color: var(--vscode-descriptionForeground);
`;

export const RecentProjectsSection = styled.section`
    max-width: 900px;
    margin: 12px auto 0;
    border: 1px solid var(--vscode-panel-border);
    border-radius: 12px;
    background: var(--vscode-editor-background);
    overflow: hidden;
`;

export const RecentProjectsHeader = styled.div`
    display: flex;
    align-items: center;
    padding: 14px 18px;
    border-bottom: 1px solid var(--vscode-panel-border);
`;

export const RecentProjectsTitle = styled.h3`
    font-size: 13px;
    font-weight: 600;
    color: var(--vscode-foreground);
    margin: 0;
`;

export const ViewAllButton = styled.button`
    font-size: 13px;
    background: none;
    border: none;
    color: var(--vscode-textLink-foreground);
    cursor: pointer;
    padding: 0;
    margin-left: auto;

    &:hover {
        color: var(--vscode-textLink-activeForeground);
        text-decoration: underline;
    }
`;

export const ProjectsList = styled.div`
    display: flex;
    flex-direction: column;
`;

export const ProjectItem = styled.button`
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
    width: 100%;
    border: none;
    background: transparent;
    text-align: left;
    padding: 12px 18px;
    font-size: 13px;
    cursor: pointer;

    &:hover {
        background: var(--vscode-list-hoverBackground);
    }
`;

export const ProjectName = styled.span`
    display: block;
    font-weight: 500;
    color: var(--vscode-foreground);
`;

export const ProjectPath = styled.span`
    display: block;
    color: var(--vscode-descriptionForeground);
    font-size: 12px;
    max-width: 100%;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
`;

export const RecentProjectsEmptyState = styled.div`
    font-size: 13px;
    color: var(--vscode-descriptionForeground);
    padding: 18px;
`;
