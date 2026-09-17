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
    YEARLY_AUDIT,
    REPORTS_ANALYTICS,
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
                            text = "Mathematical Engine, Multi-Year Compounding & Analytics Specs",
                            fontSize = 12.5.sp,
                            color = TextMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(CanvasLight, CanvasLight.copy(alpha = 0f))
                            )
                        )
                )
            }

            // 2. SCROLLABLE ACCORDION CONTAINER
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
                        expandedSection = if (expandedSection == GuideAccordionSection.ARCHITECTURE) GuideAccordionSection.NONE else GuideAccordionSection.ARCHITECTURE
                    }
                ) {
                    GuideTextParagraph("MyFin Vault is engineered around a strict offline-first local ledger model. The application maintains zero external cloud databases, zero telemetry, and zero network trackers. All financial records stay strictly on your physical device.")

                    GuideFeatureBullet(
                        title = "Local Data Sovereignty",
                        desc = "Every transaction, budget target, and account record is committed directly to on-device SQLite storage via Android Room persistence."
                    )
                    GuideFeatureBullet(
                        title = "Hardware-Backed Biometrics",
                        desc = "Biometric authentication uses native BiometricPrompt and Android KeyStore hardware crypto sandboxes. Cryptographic keys never leave the secure enclave."
                    )
                    GuideFeatureBullet(
                        title = "Corporate Float Segregation",
                        desc = "Business travel, client dining, and reimbursable outlays are recorded under a dedicated first-class Corporate flow. They reduce physical bank balances without contaminating personal lifestyle spending or inflating earned personal income."
                    )
                    GuideFeatureBullet(
                        title = "Non-Personal Inflow Deduction",
                        desc = "Capital drawdowns, emergency FD liquidations, tax refunds, and loan repayments received are systematically deducted from total inflow so only pure earned income informs your living budget."
                    )
                    GuideFeatureBullet(
                        title = "Anti-Spy Window Guard (FLAG_SECURE)",
                        desc = "Blocks OS-level screenshots, screen recording, and prevents recent task preview snapshots in Android's App Switcher."
                    )
                }

                // Section 2: Vault Operating Modes & Automated Engines
                GuideAccordionCard(
                    icon = Icons.Default.Layers,
                    title = "2. Vault Operating Modes & Engines",
                    subtitle = "3-Vault Strategy, Payday Waterfall & Sweeps",
                    isExpanded = expandedSection == GuideAccordionSection.VAULT_MODES,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == GuideAccordionSection.VAULT_MODES) GuideAccordionSection.NONE else GuideAccordionSection.VAULT_MODES
                    }
                ) {
                    GuideTextParagraph("You can switch between two capital segregation frameworks in Settings and Vault Hub at any time:")

                    GuideSubheading("A. 3-Vault Strategy (Recommended)")
                    GuideFeatureBullet(
                        title = "Operating Vault Tier",
                        desc = "Absorbs day-to-day variable lifestyle expenses (groceries, leisure, transport, dining). Holds a living runway buffer calibrated to daily burn velocity."
                    )
                    GuideFeatureBullet(
                        title = "Commitments Vault Tier",
                        desc = "Reserved strictly for contractual obligations (AutoPay bills, rent, EMIs, insurance, recurring subscriptions) so non-negotiable cash cannot be accidentally spent."
                    )
                    GuideFeatureBullet(
                        title = "Fortress Vault Tier",
                        desc = "Long-term emergency cushion. Balances up to your safety threshold remain liquid, while excess surplus compounds in fixed deposit reserves."
                    )
                    GuideFeatureBullet(
                        title = "Physical Cash Tier",
                        desc = "Physical wallet buffer for cash transactions and petty daily expenses."
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    GuideSubheading("B. Automated Allocation Engines")
                    GuideFeatureBullet(
                        title = "Dynamic Salary & Payday Engine",
                        desc = "Automatically detects recurring primary salary credits and counts down the exact days until your next payday based on historical credit patterns."
                    )
                    GuideFeatureBullet(
                        title = "Payday Waterfall Allocation Plan",
                        desc = "When salary arrives in 3-Vault mode, the engine suggests a 4-tier transfer plan: (1) Fund Operating living buffer, (2) Eliminate Commitments shortfall, (3) Route target surplus to Fortress, (4) Retain remaining balance in Operating."
                    )
                    GuideFeatureBullet(
                        title = "Month-End Wealth Sweep Plan",
                        desc = "From the 28th of each month, the system evaluates surplus operating cash above upcoming runway needs (days left + days to next payday) and suggests sweeping excess funds into Fortress compounding."
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    GuideSubheading("C. Simple Mode")
                    GuideTextParagraph("Aggregates all connected bank accounts and cash balances into a single flat liquidity pool without segregated reserve tiers.")
                }

                // Section 3: MAB Floors & Liquid Surplus
                GuideAccordionCard(
                    icon = Icons.Default.AccountBalance,
                    title = "3. MAB Floors & Spendable Surplus",
                    subtitle = "Minimum Balance Protection & Shortfall Alerts",
                    isExpanded = expandedSection == GuideAccordionSection.MAB_AND_SURPLUS,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == GuideAccordionSection.MAB_AND_SURPLUS) GuideAccordionSection.NONE else GuideAccordionSection.MAB_AND_SURPLUS
                    }
                ) {
                    GuideTextParagraph("MyFin Vault enforces strict protection for bank minimum average balance (MAB) requirements across all active accounts:")

                    GuideFeatureBullet(
                        title = "Protected MAB Floor",
                        desc = "Each bank account can be assigned a minimum balance floor to prevent non-maintenance bank penalty charges."
                    )
                    GuideFeatureBullet(
                        title = "Spendable Surplus Calculation",
                        desc = "Surplus = max(0, Current Balance - MAB). Ensures allocation engines only suggest truly disposable cash rather than dipping into required banking floors."
                    )
                    GuideFeatureBullet(
                        title = "Commitments Shortfall Engine",
                        desc = "Checks if Commitments account balance covers upcoming scheduled bills. If deficit exists, it surfaces an urgent warning with the earliest due date and 1-tap transfer prompts."
                    )
                    GuideFeatureBullet(
                        title = "Pure Guilt-Free Safe-to-Spend (Option 2)",
                        desc = "Anchored directly to verified physical cash in Operating accounts above MAB, deducting queued unpaid bills, company advances held, and living runway buffers until next payday."
                    )
                }

                // Section 4: Mathematical Engine & Core Formulas
                GuideAccordionCard(
                    icon = Icons.Default.Functions,
                    title = "4. Mathematical Engine & Formulas",
                    subtitle = "S2S, Runways, Wealth Goals & Net Worth",
                    isExpanded = expandedSection == GuideAccordionSection.MATHEMATICAL_FORMULAS,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == GuideAccordionSection.MATHEMATICAL_FORMULAS) GuideAccordionSection.NONE else GuideAccordionSection.MATHEMATICAL_FORMULAS
                    }
                ) {
                    GuideSubheading("A. Pure Personal Income Determination")
                    GuideFormulaBox(
                        formula = "I_personal = max(0, I_actual - Σ Non_Personal_Inflows)\nNon_Personal = Loan_Paybacks + Tax_Refunds + Capital_Drawdowns",
                        explanation = "Isolates true earned income. Excludes debt repayments received, tax returns, and investment liquidations so capital recycling never artificially inflates your living budget."
                    )

                    GuideSubheading("B. Safe-to-Spend (S2S) Cash-Floor Formula")
                    GuideFormulaBox(
                        formula = "S2S = max(0, Liquid_Operating_Above_MAB - Unpaid_Bills - Advance_Held - Runway_Protection)\nRunway_Protection = (Daily_Burn_Velocity) × (Days_Until_Payday)",
                        explanation = "Guarantees you never accidentally spend rent money, company advance money, or the funds needed to survive until your next verified salary credit."
                    )

                    GuideSubheading("C. Annual Wealth Accumulation Milestone Target")
                    GuideFormulaBox(
                        formula = "W_target = max(I_personal × 0.25, Fortress_Target)\nW_accumulated = Total_Yearly_Assets + max(0, Annual_Net_Surplus)",
                        explanation = "Benchmarks accumulating at least 25% of annual personal earnings, with your Fortress emergency fund acting as the absolute floor so long-term milestones remain robust."
                    )

                    GuideSubheading("D. Realizable Net Worth vs. Solvency Distribution")
                    GuideFormulaBox(
                        formula = "Realizable_Net_Worth = Liquid_Reserves + Total_Investments + Active_Receivables\nGross_Wealth = Realizable_Net_Worth + NPA_Bad_Debt_Written_Off",
                        explanation = "Distinguishes between collectible, appreciating capital and non-performing debt (NPA). Uncollectible loans are written off from Net Worth to protect balance sheet integrity."
                    )

                    GuideSubheading("E. Net Capital Retained & Retention Rate")
                    GuideFormulaBox(
                        formula = "R_net = (I_personal - E_lifestyle) - A_genuine\nRetention_% = (R_net / I_personal) × 100",
                        explanation = "Measures true preserved wealth after deducting living burn (E_lifestyle) and genuine investment assets (A_genuine, excluding peer-to-peer loans given out) from earned income."
                    )

                    GuideSubheading("F. 12-Month Extrapolated Run-Rate Velocity")
                    GuideFormulaBox(
                        formula = "Projected_Annual = (YTD_Amount / Active_Elapsed_Months) × 12\nProjected_Surplus = Proj_Inflow - Proj_Burn - Proj_Assets",
                        explanation = "Extrapolates completed month pacing across a full 12-month calendar cycle to project your year-end financial position."
                    )

                    GuideSubheading("G. Corporate Float & Advance Ring-Fence")
                    GuideFormulaBox(
                        formula = "Float_Pending = max(0, Work_Outlays - Claims_Received)\nAdvance_Held = max(0, Claims_Received - Work_Outlays)",
                        explanation = "Work expenses reduce bank balances but are ring-fenced from personal living burn. Company advances held are quarantined from spendable liquidity."
                    )
                }

                // Section 5: Yearly Fiscal Audit & Wealth Compounding
                GuideAccordionCard(
                    icon = Icons.Default.DateRange,
                    title = "5. Yearly Fiscal Audit & Compounding",
                    subtitle = "Cashflow Dynamics, Mountain Layers & Pareto",
                    isExpanded = expandedSection == GuideAccordionSection.YEARLY_AUDIT,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == GuideAccordionSection.YEARLY_AUDIT) GuideAccordionSection.NONE else GuideAccordionSection.YEARLY_AUDIT
                    }
                ) {
                    GuideTextParagraph("The Yearly module provides macro fiscal auditing across a 4-tab horizontal pager synchronized with 6 live calculation matrices:")

                    GuideSubheading("Tab 0: Cashflow Dynamics")
                    GuideFeatureBullet(
                        title = "Dual Smooth Wave with Touch Scrubber",
                        desc = "Cubic Bézier wave comparing monthly personal inflow against lifestyle burn. Slide across the canvas to inspect exact monthly surplus and retention rates."
                    )
                    GuideFeatureBullet(
                        title = "12-Month Cashflow Pulse & 3-Pillar Allocation",
                        desc = "Sparkline ledger showing surplus vs deficit months, paired with annual 50/30/20 capital deployment tracking."
                    )
                    GuideFeatureBullet(
                        title = "Fiscal Quarter Retention Grid (Q1–Q4)",
                        desc = "Breaks annual performance into 3-month rhythms to spot seasonal spending spikes and bonus accumulations."
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    GuideSubheading("Tab 1: 12-Month Cycles & Outflow Mountain")
                    GuideFeatureBullet(
                        title = "3-Layer Outflow Mountain Silhouette",
                        desc = "Stacked silhouette displaying Fixed Commitments (slate base), Lifestyle Burn (violet middle), and Assets SIP (cyan crest). Filter chips isolate individual streams."
                    )
                    GuideFeatureBullet(
                        title = "Adaptive Cycle Spotlight",
                        desc = "Compares your leanest spending month against your peak burn month, automatically adapting to single-month pacing when starting a new accounting year."
                    )
                    GuideFeatureBullet(
                        title = "Chronological Milestone Timeline",
                        desc = "Connected monthly cards with burn benchmark badges, 3-pillar micro progress bars, and corporate travel float tags."
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    GuideSubheading("Tab 2: Assets & Solvency")
                    GuideFeatureBullet(
                        title = "Animated Liquid Heart Goal Canvas",
                        desc = "Dual sine-wave animated liquid heart showing real-time progress toward your 25% annual wealth accumulation milestone."
                    )
                    GuideFeatureBullet(
                        title = "Multi-Year Compounding Pillars (Option A)",
                        desc = "Tracks multi-year asset stock accumulation (Investments + Liquid Reserves - Capital Drawdowns) with pre-seeded YoY growth percentages."
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    GuideSubheading("Tab 3: Annual Spending Pareto & Audit")
                    GuideFeatureBullet(
                        title = "Organic Curved Star Radar",
                        desc = "Audits category spending concentration using the 80/20 Pareto principle. Automatically adapts to a proportional bar when fewer than 3 categories exist."
                    )
                    GuideFeatureBullet(
                        title = "Budget vs. Actual Variance Pillars",
                        desc = "Compares realized annual spend against annualized planner targets (Monthly Target × 12), prioritizing overruns."
                    )
                }

                // Section 6: Reports & Multi-Span Analytics Hub
                GuideAccordionCard(
                    icon = Icons.Default.BarChart,
                    title = "6. Reports & Multi-Span Analytics Hub",
                    subtitle = "Timeframe Pipelines, Burn Velocity & Rhythms",
                    isExpanded = expandedSection == GuideAccordionSection.REPORTS_ANALYTICS,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == GuideAccordionSection.REPORTS_ANALYTICS) GuideAccordionSection.NONE else GuideAccordionSection.REPORTS_ANALYTICS
                    }
                ) {
                    GuideTextParagraph("The Reports & Analytics Hub analyzes spending density and velocity rhythms across customizable time intervals:")

                    GuideFeatureBullet(
                        title = "Dynamic Timeframe Selector",
                        desc = "Toggle between This Week, This Month, Last Month, and This Year. All charts, averages, and rosters update instantly."
                    )
                    GuideFeatureBullet(
                        title = "Multi-Span Cumulative Trajectories (W, M, 3 M, 6 M, Y)",
                        desc = "Plots cumulative spending burn-down curves against target limits across weekly, monthly, quarterly, semi-annual, and yearly spans."
                    )
                    GuideFeatureBullet(
                        title = "Concentric Allocation Rings",
                        desc = "Dual-ring geometry comparing fixed AutoPay obligations against variable discretionary spending."
                    )
                    GuideFeatureBullet(
                        title = "Velocity Frequency Density Strip",
                        desc = "28-day micro impulse strip mapping purchasing friction to help establish low-spend recovery days."
                    )
                    GuideFeatureBullet(
                        title = "50 / 30 / 20 Symmetrical Funnel Ribbon",
                        desc = "Displays proportion bands for Needs (50%), Wants (30%), and Asset SIPs (20%) with live percentages."
                    )
                    GuideFeatureBullet(
                        title = "Month-over-Month Velocity Surge Detection",
                        desc = "Automatically flags any lifestyle category that accelerates by more than +15% compared to the prior calendar month."
                    )
                    GuideFeatureBullet(
                        title = "Universal Graph Information Sheets",
                        desc = "Tapping any visualization opens a detailed sheet with mathematical formulas, active numbers, and interpretation advice."
                    )
                }

                // Section 7: Screen Directory & Navigation
                GuideAccordionCard(
                    icon = Icons.Default.TouchApp,
                    title = "7. Screen Directory & Navigation",
                    subtitle = "Monthly, Planner, Master DB, Analytics & Vaults",
                    isExpanded = expandedSection == GuideAccordionSection.SCREEN_DIRECTORY,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == GuideAccordionSection.SCREEN_DIRECTORY) GuideAccordionSection.NONE else GuideAccordionSection.SCREEN_DIRECTORY
                    }
                ) {
                    GuideFeatureBullet(
                        title = "Monthly Dashboard",
                        desc = "Real Liquid Safe-to-Spend display, lifestyle burn velocity sparklines, AutoPay bill checklist, and a 4-way Category Matrix (Expenses, Income, Assets/SIP, Corporate)."
                    )
                    GuideFeatureBullet(
                        title = "Yearly Fiscal View",
                        desc = "4-tab macro audit hub: Cashflow Dynamics, 12-Month Outflow Mountain, Assets & Solvency, and Annual Spending Pareto."
                    )
                    GuideFeatureBullet(
                        title = "Budget Planner",
                        desc = "Pre-allocate planned limits for Expenses, Income, Assets, and Corporate float. Features 1-click previous month cloning and AutoPay floor protection."
                    )
                    GuideFeatureBullet(
                        title = "Taxonomy Master DB",
                        desc = "Full management for Categories and Subcategories across all transaction types, with cascading historical SQLite updates."
                    )
                    GuideFeatureBullet(
                        title = "Reports & Analytics Hub",
                        desc = "Timeframe-driven 3-tab hub: Summary Analytics (trajectories, concentric rings), Categories Analytics (radar web, 50/30/20 ribbon), and Wealth Analytics (reserve mountain, 3-bubble distribution)."
                    )
                    GuideFeatureBullet(
                        title = "Vault Accounts Hub & Carousel",
                        desc = "Swipeable physical bank cards with card reordering, MAB badge editing, sweep threshold calibration, and instant inter-vault transfers."
                    )
                }

                // Section 8: Data Backup & Export Engines
                GuideAccordionCard(
                    icon = Icons.Default.SaveAlt,
                    title = "8. Backup, Restore & Exports",
                    subtitle = "Full .json snapshots, .xlsx Workbooks, .csv Ledgers",
                    isExpanded = expandedSection == GuideAccordionSection.BACKUP_EXPORTS,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == GuideAccordionSection.BACKUP_EXPORTS) GuideAccordionSection.NONE else GuideAccordionSection.BACKUP_EXPORTS
                    }
                ) {
                    GuideFeatureBullet(
                        title = "Full Vault Snapshot (.json)",
                        desc = "Serializes the complete database (UserProfile, Transactions, Categories, Accounts, Fixed Bills, Budget Plans) into an offline JSON backup for migrations and restores."
                    )
                    GuideFeatureBullet(
                        title = "Accounting Statement (.xlsx)",
                        desc = "Generates a styled Excel workbook containing Monthly Statements, Category Rollups, and Vault Account Balances."
                    )
                    GuideFeatureBullet(
                        title = "Universal Flat Ledger (.csv)",
                        desc = "Standard UTF-8 comma-separated export formatted with Byte Order Mark (\\uFEFF) for compatibility across Excel, Numbers, and Google Sheets."
                    )
                    GuideFeatureBullet(
                        title = "PDF Financial Statements",
                        desc = "Exports formatted periodic summary statements directly from the Reports & Analytics dock."
                    )
                }

                // Section 9: Mathematical Legend & Symbol Index
                GuideAccordionCard(
                    icon = Icons.Default.FormatListNumbered,
                    title = "9. Mathematical Legend & Symbols",
                    subtitle = "Reference Table of Arithmetic Variables",
                    isExpanded = expandedSection == GuideAccordionSection.SYMBOL_LEGEND,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == GuideAccordionSection.SYMBOL_LEGEND) GuideAccordionSection.NONE else GuideAccordionSection.SYMBOL_LEGEND
                    }
                ) {
                    GuideSymbolRow(symbol = "I_personal", meaning = "Pure Personal Inflow", formula = "I_actual - NonPersonal_Inflow")
                    GuideSymbolRow(symbol = "E_lifestyle", meaning = "Pure Lifestyle Outflow", formula = "Type == EXPENSE (Pure living)")
                    GuideSymbolRow(symbol = "C_fixed", meaning = "Total Fixed Commitments", formula = "Σ Bills_(EXPENSE + TRANSFER)")
                    GuideSymbolRow(symbol = "C_pending", meaning = "Queued Unpaid Commitments", formula = "Σ Unpaid_Bills + Pending_SIP")
                    GuideSymbolRow(symbol = "MAB", meaning = "Minimum Average Balance Floor", formula = "Protected Account Minimum")
                    GuideSymbolRow(symbol = "Surplus", meaning = "Spendable Cash Above Floor", formula = "max(0, Balance - MAB)")
                    GuideSymbolRow(symbol = "S2S", meaning = "Liquid Safe-to-Spend", formula = "Cash_Floor - Runway_Protection")
                    GuideSymbolRow(symbol = "W_target", meaning = "Annual Wealth Goal", formula = "max(I_personal × 0.25, Fortress)")
                    GuideSymbolRow(symbol = "RNW", meaning = "Realizable Net Worth", formula = "Liquid + Invested + Receivables")
                    GuideSymbolRow(symbol = "GW", meaning = "Gross Wealth (Audit)", formula = "RNW + NPA_Bad_Debt")
                    GuideSymbolRow(symbol = "NPA", meaning = "Non-Performing Assets", formula = "Written-off bad personal debt")
                    GuideSymbolRow(symbol = "F_corp", meaning = "Pending Corporate Claims", formula = "max(0, Outlays - Claims)")
                    GuideSymbolRow(symbol = "R_net", meaning = "Net Retained Capital", formula = "(I_personal - E_lifestyle) - A_genuine")
                    GuideSymbolRow(symbol = "V_daily", meaning = "Daily Burn Velocity", formula = "Period_Spend / Total_Days")
                    GuideSymbolRow(symbol = "M_runway", meaning = "Emergency Cushion Months", formula = "Liquid Vaults / Monthly_Burn")
                }

                Spacer(modifier = Modifier.height(6.dp))

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
