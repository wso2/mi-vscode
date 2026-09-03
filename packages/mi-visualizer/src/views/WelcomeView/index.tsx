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

import { useEffect, useState } from "react";
import { MACHINE_VIEW, EVENT_TYPE, RecentProjectEntry } from "@wso2/mi-core";
import { useVisualizerContext } from "@wso2/mi-rpc-client";
import { Codicon } from "@wso2/ui-toolkit";
import { COMMANDS } from "../../constants";
import {
    ActionCard,
    Caption,
    CardDescription,
    CardIcon,
    CardsContainer,
    CardsGrid,
    CardTitle,
    Headline,
    Hero,
    MoreDivider,
    MoreToggleButton,
    MoreToggleWrapper,
    PrimaryCardButton,
    ProjectItem,
    ProjectName,
    ProjectPath,
    ProjectsList,
    RecentProjectsEmptyState,
    RecentProjectsHeader,
    RecentProjectsSection,
    RecentProjectsTitle,
    SecondaryActionRow,
    SecondaryCardButton,
    SecondaryCardsSection,
    SecondaryRowContent,
    SecondaryRowDescription,
    SecondaryRowIcon,
    SecondaryRowTitle,
    ViewAllButton,
    Wrapper,
} from "./styles";

export function WelcomeView() {
    const { rpcClient } = useVisualizerContext();
    const [isConsolidatedProject, setIsConsolidatedProject] = useState<boolean | undefined>(undefined);
    const [showSecondary, setShowSecondary] = useState(false);
    const [recentProjects, setRecentProjects] = useState<RecentProjectEntry[]>([]);
    const [isRecentProjectsLoaded, setIsRecentProjectsLoaded] = useState(false);

    useEffect(() => {
        if (!rpcClient) {
            return;
        }

        (async () => {
            try {
                const canCreate = await rpcClient.getMiDiagramRpcClient().canCreateConsolidatedProject();
                setIsConsolidatedProject(canCreate.isConsolidatedProject);
            } catch (error) {
                console.error("Failed to check consolidated project status:", error);
            }
        })();

        (async () => {
            try {
                const recent = await rpcClient.getMiVisualizerRpcClient().getRecentProjects();
                setRecentProjects(recent.projects);
            } catch (error) {
                console.error("Failed to fetch recent projects:", error);
            } finally {
                setIsRecentProjectsLoaded(true);
            }
        })();
    }, [rpcClient]);

    const goToCreateProject = () => {
        rpcClient.getMiVisualizerRpcClient().openView({
            type: EVENT_TYPE.OPEN_VIEW,
            location: {
                view: MACHINE_VIEW.ProjectCreationForm
            }
        });
    }

    const goToOpenProject = async () => {
        await rpcClient.getMiDiagramRpcClient().executeCommand({ commands: [COMMANDS.OPEN_PROJECT] });
    }

    const goToImportFromCApp = async () => {
        await rpcClient.getMiDiagramRpcClient().executeCommand({ commands: [COMMANDS.IMPORT_FROM_CAPP] });
    }

    const goToSamples = () => {
        rpcClient.getMiVisualizerRpcClient().openView({
            type: EVENT_TYPE.OPEN_VIEW,
            location: {
                view: MACHINE_VIEW.Samples
            }
        });
    }

    const openRecentProjectsPicker = () => {
        rpcClient.getMiDiagramRpcClient().executeCommand({ commands: ["workbench.action.openRecent"] });
    }

    const openRecentProject = (projectPath: string) => {
        rpcClient.getMiVisualizerRpcClient().openRecentProject({ path: projectPath });
    }

    return (
        <Wrapper>
            <Hero>
                <Headline>WSO2 Integrator</Headline>
                <Caption>
                    Connect AI agents, APIs, data, and events across cloud, on-prem, and hybrid environments. Build any type of integration and AI agent with the 100% open source WSO2 Integrator.
                </Caption>
            </Hero>
            <CardsContainer>
                <CardsGrid>
                    <ActionCard>
                        <CardIcon bg="linear-gradient(135deg, var(--wso2-brand-primary-alt) 0%, var(--wso2-brand-primary-deep) 100%)">
                            <Codicon name="folder-library" iconSx={{ fontSize: 22 }} />
                        </CardIcon>
                        <CardTitle>Create New Project</CardTitle>
                        <CardDescription>Create an empty project.</CardDescription>
                        <PrimaryCardButton onClick={goToCreateProject}>Create</PrimaryCardButton>
                    </ActionCard>
                    <ActionCard>
                        <CardIcon bg="linear-gradient(135deg, var(--wso2-brand-primary-alt) 0%, var(--wso2-brand-accent-alt) 100%)">
                            <Codicon name="folder-opened" iconSx={{ fontSize: 22 }} />
                        </CardIcon>
                        <CardTitle>Open Project</CardTitle>
                        <CardDescription>Open an existing integration project.</CardDescription>
                        <SecondaryCardButton onClick={goToOpenProject}>Open</SecondaryCardButton>
                    </ActionCard>
                    <ActionCard>
                        <CardIcon bg="linear-gradient(135deg, var(--wso2-brand-accent) 0%, var(--wso2-brand-accent-alt) 100%)">
                            <Codicon name="lightbulb" iconSx={{ fontSize: 22 }} />
                        </CardIcon>
                        <CardTitle>Explore Samples</CardTitle>
                        <CardDescription>Have a look at some examples to get started quickly.</CardDescription>
                        <SecondaryCardButton onClick={goToSamples}>Explore</SecondaryCardButton>
                    </ActionCard>
                </CardsGrid>

                {isConsolidatedProject === false && (
                    <>
                        <MoreToggleWrapper>
                            <MoreDivider />
                            <MoreToggleButton type="button" onClick={() => setShowSecondary(!showSecondary)}>
                                <span>{showSecondary ? "Show less" : "More Actions"}</span>
                                <span className={`codicon ${showSecondary ? "codicon-triangle-up" : "codicon-triangle-down"}`} />
                            </MoreToggleButton>
                            <MoreDivider />
                        </MoreToggleWrapper>
                        <SecondaryCardsSection
                            aria-hidden={!showSecondary}
                            style={{
                                maxHeight: showSecondary ? "200px" : "0",
                                opacity: showSecondary ? 1 : 0,
                            }}
                        >
                            <SecondaryActionRow type="button" tabIndex={showSecondary ? 0 : -1} onClick={goToImportFromCApp}>
                                <SecondaryRowIcon>
                                    <Codicon name="package" iconSx={{ fontSize: 16 }} />
                                </SecondaryRowIcon>
                                <SecondaryRowContent>
                                    <SecondaryRowTitle>Import a CApp</SecondaryRowTitle>
                                    <SecondaryRowDescription>Import a CApp to create a new project.</SecondaryRowDescription>
                                </SecondaryRowContent>
                                <Codicon name="chevron-right" iconSx={{ fontSize: 14, opacity: 0.6 }} />
                            </SecondaryActionRow>
                        </SecondaryCardsSection>
                    </>
                )}

                {isRecentProjectsLoaded && (
                    <RecentProjectsSection>
                        <RecentProjectsHeader>
                            <RecentProjectsTitle>Recent Projects</RecentProjectsTitle>
                            <ViewAllButton type="button" onClick={openRecentProjectsPicker}>See more</ViewAllButton>
                        </RecentProjectsHeader>
                        {recentProjects.length > 0 ? (
                            <ProjectsList>
                                {recentProjects.map((project) => (
                                    <ProjectItem
                                        key={project.path}
                                        type="button"
                                        onClick={() => openRecentProject(project.path)}
                                        title={project.description || project.path}
                                    >
                                        <ProjectName>{project.label}</ProjectName>
                                        <ProjectPath>{project.description || project.path}</ProjectPath>
                                    </ProjectItem>
                                ))}
                            </ProjectsList>
                        ) : (
                            <RecentProjectsEmptyState>No recent projects found.</RecentProjectsEmptyState>
                        )}
                    </RecentProjectsSection>
                )}
            </CardsContainer>
        </Wrapper>
    );
}
