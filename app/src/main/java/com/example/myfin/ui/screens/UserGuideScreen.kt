@file:OptIn(ExperimentalFoundationApi::class)

package com.example.myfin.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myfin.BuildConfig
import com.example.myfin.ui.components.AppBrandingFooter
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.delay

enum class GuideAccordionSection {
    NONE,
    ARCHITECTURE,
    VAULT_MODES,
    MAB_AND_SURPLUS,
    MATHEMATICAL_FORMULAS,
    SCREEN_DIRECTORY,
    BACKUP_EXPORTS,
    SYMBOL_LEGEND
}

@Composable
fun UserGuideScreen(
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var expandedSection by rememberSaveable { mutableStateOf(GuideAccordionSection.ARCHITECTURE) }

    // Intercept hardware and gesture back actions for a seamless exit transition
    BackHandler {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. PINNED HEADER SECTION
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(2f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CanvasLight)
                ) {
                    // Top Purple Gradient Horizon Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(125.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            AccentPurple,
                                            AccentPurple.copy(alpha = 0.88f),
                                            Color(0xFF6C5CE7).copy(alpha = 0.24f)
                                        )
                                    )
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onBack()
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.22f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = "Back to Settings",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Text(
                                    text = "Handbook v${BuildConfig.VERSION_NAME}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.92f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.18f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Overlapping Hero Icon on Horizon Line
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 24.dp)
                                .size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(76.dp),
                                shape = CircleShape,
                                color = AccentPurple,
                                border = BorderStroke(3.dp, CardWhite),
                                shadowElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Guide Title Block
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 4.dp, bottom = 12.dp)
                    ) {
                        Text(
                            text = "User Guide & Architecture",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Mathematical Engine, MAB Floors & Corporate Float Specs",
                            fontSize = 12.5.sp,
                            color = TextMuted
                        )
                    }
                }

                // Smooth Dissolve Fade Overlay at Bottom of Pinned Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    CanvasLight,
                                    CanvasLight.copy(alpha = 0f)
                                )
                            )
                        )
                )
            }

            // 2. SCROLLABLE HANDBOOK CARDS CONTAINER
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Architecture & Privacy Specs
                GuideAccordionCard(
                    icon = Icons.Default.Shield,
                    title = "1. Architecture & Privacy Specs",
                    subtitle = "100% Offline SQLite & Hardware KeyStore",
                    isExpanded = expandedSection == GuideAccordionSection.ARCHITECTURE,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == GuideAccordionSection.ARCHITECTURE) {
                            GuideAccordionSection.NONE
                        } else {
                            GuideAccordionSection.ARCHITECTURE
                        }
                    }
                ) {
                    GuideTextParagraph("MyFin Vault is engineered around a 100% offline-first local ledger model. The application maintains zero external cloud databases, zero telemetry, and zero network trackers. All financial data stays strictly on your physical device.")

                    GuideFeatureBullet(
                        title = "Local Data Sovereignty",
                        desc = "Every transaction, budget plan, and custom category is committed directly to on-device SQLite storage via Android Room persistence."
                    )
                    GuideFeatureBullet(
                        title = "Hardware-Backed Biometrics",
                        desc = "Biometric authentication uses native BiometricPrompt and Android KeyStore hardware crypto sandboxes. Cryptographic keys never leave the local secure environment."
                    )
                    GuideFeatureBullet(
                        title = "Corporate Float Segregation",
                        desc = "Business travel, client dining, and courier expenses are recorded under a dedicated first-class Corporate flow. They reduce physical bank balances without contaminating personal lifestyle spending or inflating earned personal income."
                    )
                    GuideFeatureBullet(
                        title = "Anti-Spy Window Guard (FLAG_SECURE)",
                        desc = "Blocks OS-level screenshots, screen mirroring, and prevents recent task preview snapshots in Android's App Switcher."
                    )
                }

                // Section 2: Vault Operating Modes
                GuideAccordionCard(
                    icon = Icons.Default.Layers,
                    title = "2. Vault Operating Modes",
                    subtitle = "3-Vault Strategy vs. Simple Mode",
                    isExpanded = expandedSection == GuideAccordionSection.VAULT_MODES,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == GuideAccordionSection.VAULT_MODES) {
                            GuideAccordionSection.NONE
                        } else {
                            GuideAccordionSection.VAULT_MODES
                        }
                    }
                ) {
                    GuideTextParagraph("You can switch between two capital segregation frameworks in Settings and Vault Hub at any time:")

                    GuideSubheading("A. 3-Vault Strategy (Recommended)")
                    GuideFeatureBullet(
                        title = "Operating Vault",
                        desc = "Absorbs day-to-day variable lifestyle expenses (groceries, leisure, transport, dining). Suggests sweeps when surplus exceeds your dynamic monthly runway target."
                    )
                    GuideFeatureBullet(
                        title = "Commitments Vault",
                        desc = "Reserved strictly for fixed obligations (AutoPay bills, rent, EMIs, insurance, recurring dues) so funds cannot be accidentally spent."
                    )
                    GuideFeatureBullet(
                        title = "Fortress Vault",
                        desc = "High-reserve emergency buffer. Automatically categorizes balances up to your safety threshold as liquid cushion, and sweeps excess amounts into emergency fixed deposits."
                    )
                    GuideFeatureBullet(
                        title = "Cash Wallet",
                        desc = "Physical petty cash tracking for micro-payments."
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    GuideSubheading("B. Simple Mode")
                    GuideTextParagraph("Aggregates all connected bank accounts and cash balances into a single flat liquidity pool without segregated reserve buckets.")
                }

                // Section 3: MAB Floors & Spendable Surplus
                GuideAccordionCard(
                    icon = Icons.Default.AccountBalance,
                    title = "3. MAB Floors & Spendable Surplus",
                    subtitle = "Minimum Average Balance & Automated Safety",
                    isExpanded = expandedSection == GuideAccordionSection.MAB_AND_SURPLUS,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == GuideAccordionSection.MAB_AND_SURPLUS) {
                            GuideAccordionSection.NONE
                        } else {
                            GuideAccordionSection.MAB_AND_SURPLUS
                        }
                    }
                ) {
                    GuideTextParagraph("MyFin Vault enforces protection for bank minimum balance (MAB) requirements across all active accounts:")

                    GuideFeatureBullet(
                        title = "Protected MAB Floor",
                        desc = "Each bank account can be assigned a minimum balance floor to prevent non-maintenance penalty charges."
                    )
                    GuideFeatureBullet(
                        title = "Spendable Surplus Calculation",
                        desc = "Surplus = max(0, Current Balance - MAB). This ensures allocation engines only suggest truly disposable cash rather than eating into your required banking floor."
                    )
                    GuideFeatureBullet(
                        title = "Commitments Shortfall Warnings",
                        desc = "If unpaid global outlays (bills and recurring commitments originating from the Commitments vault) exceed your balance, MyFin surfaces an urgent shortfall warning with 1-tap transfer prompts."
                    )
                    GuideFeatureBullet(
                        title = "Payday & Month-End Sweep Engines",
                        desc = "When your salary is logged in 3-Vault mode, the app calculates exact bill funding and emergency transfers. At month-end (from the 28th), any surplus remaining above your living buffer can be swept directly into the Fortress vault."
                    )
                }

                // Section 4: Mathematical Engine & Formulas
                GuideAccordionCard(
                    icon = Icons.Default.Functions,
                    title = "4. Mathematical Engine & Formulas",
                    subtitle = "Real-Time S2S, Runways, Corporate Float & Splits",
                    isExpanded = expandedSection == GuideAccordionSection.MATHEMATICAL_FORMULAS,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == GuideAccordionSection.MATHEMATICAL_FORMULAS) {
                            GuideAccordionSection.NONE
                        } else {
                            GuideAccordionSection.MATHEMATICAL_FORMULAS
                        }
                    }
                ) {
                    GuideSubheading("A. Effective Base Inflow Determination")
                    GuideFormulaBox(
                        formula = "I_base = max(I_planned, I_personal)\nI_personal = I_actual - Σ Non_Personal_Inflows",
                        explanation = "Corporate reimbursements, loan paybacks received, and capital drawdowns are excluded from personal income to ensure true budgeting accuracy."
                    )

                    GuideSubheading("B. Fixed Commitments Load")
                    GuideFormulaBox(
                        formula = "C_fixed = Σ FixedBills_EXPENSE + max(A_planned, A_actual)\nLoad_% = (C_fixed / I_base) * 100",
                        explanation = "Evaluates the percentage of monthly inflow strictly pre-committed to recurring bills and fixed asset investments."
                    )

                    GuideSubheading("C. Safe-to-Spend (S2S) Dual Engine")
                    GuideFormulaBox(
                        formula = "S2S_theoretical = max(0, I_base - C_fixed - E_discretionary)\nS2S_real = min(S2S_theoretical, Liquid_Operating_Cash - Excess_Advance)",
                        explanation = "S2S bounds theoretical budget headroom by physical cash in Operating vaults, ring-fencing pending bills, MAB floors, and any excess corporate advances held."
                    )

                    GuideSubheading("D. Net Capital Retained & Retention Rate")
                    GuideFormulaBox(
                        formula = "R_net = I_personal - E_lifestyle - A_actual\nRetention_% = (R_net / I_personal) * 100",
                        explanation = "Measures true preserved wealth after deducting living expenses (E_lifestyle) and investments (A_actual) from earned personal income."
                    )

                    GuideSubheading("E. Corporate Float & Settlement Isolation")
                    GuideFormulaBox(
                        formula = "Float_pending = max(0, Σ Work_Outlays - Σ Claims_Received)\nAdvance_held = max(0, Σ Claims_Received - Σ Work_Outlays)",
                        explanation = "Work expenses are tracked as corporate float. Outlays reduce bank balances while pending claims remain ring-fenced from personal lifestyle burn."
                    )

                    GuideSubheading("F. 50 / 30 / 20 Cashflow Split")
                    GuideFormulaBox(
                        formula = "Needs (50%) = Fixed Bills + Essential Categories\nWants (30%) = max(0, E_lifestyle - Needs)\nAssets (20%) = Σ Asset Investments",
                        explanation = "Organizes monthly personal outflow into standard macro allocations for long-term financial health."
                    )
                }

                // Section 5: Screen Directory & Navigation
                GuideAccordionCard(
                    icon = Icons.Default.TouchApp,
                    title = "5. Screen Directory & Navigation",
                    subtitle = "Monthly, Planner, Master DB, Analytics & Vaults",
                    isExpanded = expandedSection == GuideAccordionSection.SCREEN_DIRECTORY,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == GuideAccordionSection.SCREEN_DIRECTORY) {
                            GuideAccordionSection.NONE
                        } else {
                            GuideAccordionSection.SCREEN_DIRECTORY
                        }
                    }
                ) {
                    GuideFeatureBullet(
                        title = "Monthly Dashboard",
                        desc = "Real Liquid Safe-to-Spend hero display, lifestyle burn velocity sparklines, AutoPay checklist, and a 4-way Category Matrix (Expenses, Income, Assets/SIP, and Corporate)."
                    )
                    GuideFeatureBullet(
                        title = "Budget Planner",
                        desc = "Pre-allocate planned ceilings for Expenses, Income, Assets, and Corporate float. Features 1-click previous month budget cloning, AutoPay floor protection, and ceiling freezes."
                    )
                    GuideFeatureBullet(
                        title = "Taxonomy Master DB",
                        desc = "Full CRUD management for Categories and Subcategories across all four transaction types, with cascading historical SQLite updates."
                    )
                    GuideFeatureBullet(
                        title = "Reports & Analytics Hub",
                        desc = "Dedicated 3-tab dock: (1) Summary Analytics (Net Capital spline wave, concentric donut, burn velocity bars), (2) Category Analytics (6-axis radar web, 50/30/20 ribbon, corporate float card), (3) Wealth Analytics (reserve mountain chart, asset bubble map, runway gauge)."
                    )
                    GuideFeatureBullet(
                        title = "Vault Accounts Hub & Carousel",
                        desc = "Swipeable physical bank cards with card reordering, MAB badge editing, auto-sweep threshold calibration, and instant inter-vault transfers with backdated date support."
                    )
                }

                // Section 6: Data Backup & Export Engines
                GuideAccordionCard(
                    icon = Icons.Default.SaveAlt,
                    title = "6. Backup, Restore & Exports",
                    subtitle = "Full .json snapshots, .xlsx Workbooks, .csv Ledgers",
                    isExpanded = expandedSection == GuideAccordionSection.BACKUP_EXPORTS,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == GuideAccordionSection.BACKUP_EXPORTS) {
                            GuideAccordionSection.NONE
                        } else {
                            GuideAccordionSection.BACKUP_EXPORTS
                        }
                    }
                ) {
                    GuideFeatureBullet(
                        title = "Full Vault Snapshot (.json)",
                        desc = "Serializes the complete database (UserProfile, Transactions, Categories, Accounts, Fixed Bills, Budget Plans) into an offline encrypted JSON snapshot for seamless migrations and restores."
                    )
                    GuideFeatureBullet(
                        title = "Accounting Statement (.xlsx)",
                        desc = "Generates a styled Excel workbook containing Monthly Statements, Category Rollups, and Vault Account Balances."
                    )
                    GuideFeatureBullet(
                        title = "Universal Flat Ledger (.csv)",
                        desc = "Standard UTF-8 comma-separated export formatted with Byte Order Mark (\\uFEFF) for compatibility across Excel, Numbers, and Google Sheets."
                    )
                }

                // Section 7: Mathematical Legend & Symbol Index
                GuideAccordionCard(
                    icon = Icons.Default.FormatListNumbered,
                    title = "7. Mathematical Legend & Symbols",
                    subtitle = "Reference Table of Arithmetic Variables",
                    isExpanded = expandedSection == GuideAccordionSection.SYMBOL_LEGEND,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == GuideAccordionSection.SYMBOL_LEGEND) {
                            GuideAccordionSection.NONE
                        } else {
                            GuideAccordionSection.SYMBOL_LEGEND
                        }
                    }
                ) {
                    GuideSymbolRow(symbol = "I_personal", meaning = "Pure Personal Inflow", formula = "I_actual - NonPersonal_Inflow")
                    GuideSymbolRow(symbol = "E_lifestyle", meaning = "Pure Lifestyle Outflow", formula = "E_actual - Work_Outlays")
                    GuideSymbolRow(symbol = "C_fixed", meaning = "Total Fixed Commitments", formula = "Σ Bills_EXPENSE + max(A_plan, A_act)")
                    GuideSymbolRow(symbol = "MAB", meaning = "Minimum Average Balance Floor", formula = "Protected Account Minimum")
                    GuideSymbolRow(symbol = "Surplus", meaning = "Spendable Cash Above Floor", formula = "max(0, Balance - MAB)")
                    GuideSymbolRow(symbol = "S2S", meaning = "Safe-to-Spend Liquidity", formula = "min(S2S_theo, Operating_Cash - Adv)")
                    GuideSymbolRow(symbol = "F_corp", meaning = "Pending Corporate Claims", formula = "max(0, Outlays - Claims)")
                    GuideSymbolRow(symbol = "R_net", meaning = "Net Retained Capital", formula = "I_personal - E_lifestyle - A_actual")
                    GuideSymbolRow(symbol = "M_runway", meaning = "Emergency Cushion Months", formula = "Liquid Vaults / max(1.0, Monthly_Burn)")
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Shared App Branding Footer
                AppBrandingFooter(
                    modifier = Modifier.fillMaxWidth(),
                    version = "v${BuildConfig.VERSION_NAME}",
                    showIcon = true
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ==========================================
// ACCORDION & FORMATTING COMPONENTS
// ==========================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GuideAccordionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            delay(220)
            bringIntoViewRequester.bringIntoView()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .shadow(3.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = CardWhite
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AccentPurple.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = title,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = subtitle,
                            fontSize = 11.5.sp,
                            color = TextMuted
                        )
                    }
                }

                val rotationState by animateFloatAsState(
                    targetValue = if (isExpanded) 90f else 0f,
                    label = "chevronRotation"
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(rotationState)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(
                        color = BorderLight.copy(alpha = 0.4f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    content()
                }
            }
        }
    }
}

@Composable
private fun GuideTextParagraph(text: String) {
    Text(
        text = text,
        fontSize = 12.5.sp,
        color = TextDark,
        lineHeight = 17.5.sp
    )
}

@Composable
private fun GuideSubheading(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = AccentPurple,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun GuideFeatureBullet(title: String, desc: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(AccentPurple)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }
        Text(
            text = desc,
            fontSize = 12.sp,
            color = TextMuted,
            lineHeight = 16.sp,
            modifier = Modifier.padding(start = 13.dp, top = 2.dp)
        )
    }
}

@Composable
private fun GuideFormulaBox(formula: String, explanation: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AccentPurple.copy(alpha = 0.06f),
        border = BorderStroke(0.8.dp, AccentPurple.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = formula,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = AccentPurple,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = explanation,
                fontSize = 11.5.sp,
                color = TextDark,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun GuideSymbolRow(symbol: String, meaning: String, formula: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = CanvasLight,
        border = BorderStroke(0.6.dp, BorderLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = symbol,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = AccentPurple
                )
                Text(
                    text = meaning,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            Text(
                text = formula,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark
            )
        }
    }
}
