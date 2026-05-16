#!/usr/bin/env python3
import pandas as pd
import numpy as np
from scipy import stats

bench_csv = 'mcaa_test_reports/20260510_171608_benchmark_results.csv'
df = pd.read_csv(bench_csv)
print(f'Benchmark: {len(df)} rows')

print('\n=== PER-ALGORITHM STATISTICS (mean ± std) ===')
for algo in ['MCAA', 'Vickrey', 'DoubleAuction', 'LP']:
    sub = df[df['algorithm'] == algo]
    n = len(sub)
    svc_mean, svc_std = sub['serviceLevel'].mean(), sub['serviceLevel'].std()
    cost_mean, cost_std = sub['totalCost'].mean(), sub['totalCost'].std()
    time_mean, time_std = sub['executionTimeMs'].mean(), sub['executionTimeMs'].std()
    gini_mean, gini_std = sub['giniCoefficient'].mean(), sub['giniCoefficient'].std()
    print(f'{algo:>16s}: SL={svc_mean:.4f}±{svc_std:.4f}  Cost={cost_mean:7.0f}±{cost_std:5.0f}  Time={time_mean:.1f}±{time_std:.1f}ms  Gini={gini_mean:.4f}±{gini_std:.4f}')

print('\n=== PAIRED T-TESTS: MCAA vs competitors ===')
mcaa_df = df[df['algorithm'] == 'MCAA']
for competitor in ['Vickrey', 'DoubleAuction', 'LP']:
    comp_df = df[df['algorithm'] == competitor]
    merged = pd.merge(mcaa_df, comp_df, on=['seed','numOrders','numResources','etaVariance','weightStrategy'],
                      suffixes=('_mcaa','_comp'))
    for metric in ['serviceLevel', 'totalCost']:
        a = merged[f'{metric}_mcaa']
        b = merged[f'{metric}_comp']
        t_stat, p_val = stats.ttest_rel(a, b)
        mean_diff = a.mean() - b.mean()
        sig = '***' if p_val < 0.001 else '**' if p_val < 0.01 else '*' if p_val < 0.05 else 'ns'
        print(f'  MCAA vs {competitor:>14s} on {metric:>12s}: diff={mean_diff:+.4f}  t={t_stat:.3f}  p={p_val:.6f}  {sig}')

print('\n=== MCAA BY WEIGHT STRATEGY ===')
for ws in ['balanced', 'costHeavy', 'timeHeavy']:
    sub = df[(df['algorithm']=='MCAA') & (df['weightStrategy']==ws)]
    print(f'  MCAA x {ws:>10s}: SL={sub["serviceLevel"].mean():.4f}±{sub["serviceLevel"].std():.4f}  Cost={sub["totalCost"].mean():7.0f}±{sub["totalCost"].std():5.0f}')

print('\n=== SERVICE LEVEL BY ETA VARIANCE vs ALGORITHM ===')
pivot_svc = df.pivot_table(index='etaVariance', columns='algorithm', values='serviceLevel', aggfunc='mean')
pivot_svc = pivot_svc.reindex(index=[0.0, 0.1, 0.2, 0.3])
print(pivot_svc.to_string(float_format='.4f'))

print('\n=== TOTAL COST BY ETA VARIANCE vs ALGORITHM ===')
pivot_cost = df.pivot_table(index='etaVariance', columns='algorithm', values='totalCost', aggfunc='mean')
pivot_cost = pivot_cost.reindex(index=[0.0, 0.1, 0.2, 0.3])
print(pivot_cost.to_string(float_format='.0f'))

print('\n=== SERVICE LEVEL BY ETA VARIANCE vs WEIGHT STRATEGY (ALL) ===')
pivot_heat = df.pivot_table(index='etaVariance', columns='weightStrategy', values='serviceLevel', aggfunc='mean')
pivot_heat = pivot_heat.reindex(index=[0.0, 0.1, 0.2, 0.3], columns=['balanced','costHeavy','timeHeavy'])
print(pivot_heat.to_string(float_format='.4f'))

print('\n=== PARETO FRONTIER (per seed averages) ===')
agg = df.groupby(['algorithm','seed']).agg(serviceLevel=('serviceLevel','mean'), totalCost=('totalCost','mean')).reset_index()
all_pts = agg[['serviceLevel','totalCost']].values
pareto_mask = np.ones(len(all_pts), dtype=bool)
for i in range(len(all_pts)):
    for j in range(len(all_pts)):
        if i != j and all_pts[j,1] <= all_pts[i,1] and all_pts[j,0] >= all_pts[i,0]:
            if all_pts[j,1] < all_pts[i,1] or all_pts[j,0] > all_pts[i,0]:
                pareto_mask[i] = False
                break
pareto_pts = all_pts[pareto_mask]
pareto_sorted = pareto_pts[np.argsort(pareto_pts[:,1])]
for pt in pareto_sorted[:10]:
    print(f'  SL={pt[0]:.4f}  Cost={pt[1]:.0f}')
print(f'  ... ({len(pareto_sorted)} Pareto-optimal points total)')

print('\n=== REAL-WORLD DATASET RESULTS ===')
for ds_name, csv_file in [
    ('Olist', 'mcaa_test_reports/20260510_171609_olist_results.csv'),
    ('Supply Chain', 'mcaa_test_reports/20260510_171611_supplychain_results.csv'),
    ('INCOM2024', 'mcaa_test_reports/20260510_171614_incom_results.csv'),
    ('TLC', 'mcaa_test_reports/20260510_171617_tlc_results.csv'),
]:
    ddf = pd.read_csv(csv_file)
    n = ddf.iloc[0]['numOrders']
    print(f'\n  {ds_name} (n={n}):')
    for _, row in ddf.iterrows():
        algo = row['algorithm']
        sl = row['serviceLevel']
        cost = row['totalCost']
        t = row['executionTimeMs']
        gini = row['giniCoefficient']
        print(f'    {algo:>14s}: SL={sl:.4f}  Cost={cost:10.0f}  Time={t:5.0f}ms  Gini={gini:.4f}')

print('\n=== EXECUTION TIME BY ETA VARIANCE vs ALGORITHM ===')
pivot_time = df.pivot_table(index='etaVariance', columns='algorithm', values='executionTimeMs', aggfunc='mean')
pivot_time = pivot_time.reindex(index=[0.0, 0.1, 0.2, 0.3])
print(pivot_time.to_string(float_format='.1f'))
print('\nDone.')