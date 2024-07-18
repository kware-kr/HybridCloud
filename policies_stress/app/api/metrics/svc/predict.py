import sqlite3
import warnings
from typing import Iterable, List

import numpy as np
import pandas as pd
from statsmodels.tsa.api import VAR
from statsmodels.tsa.vector_ar.var_model import VARResults

from api.metrics.data.model import Metric

warnings.filterwarnings('ignore')


# 예측
def forecast(data: Iterable[sqlite3.Row], steps) -> List[Metric]:
    df = _to_dataframe(data)

    model = _build_var_model(df)
    predictions = model.forecast(df.values, steps=steps)

    index = [df.index[-1] + np.timedelta64((i + 1) * 15, 's') for i in range(steps)]
    pred_df = pd.DataFrame(predictions, index=index, columns=df.columns)

    results = []
    for i, x in enumerate(pred_df.values):
        results.append(Metric(
            date_created=index[i],
            cpu_usage=x[0],
            dsk_usage=x[1],
            mem_usage=x[2],
            gpu_usage=x[3],
            dsk_io=x[4],
            is_predict=True
        ))
    return results


# 모델 학습
def _build_var_model(data: pd.DataFrame) -> VARResults:
    model = VAR(data)

    # Akaike 정보 기준 사용, 최대 시차 설정
    maxlags = int(data.shape[0] * .2195)
    selections = model.select_order(maxlags=maxlags)
    aic = selections.selected_orders['aic']
    model_fit = model.fit(ic='aic', maxlags=aic)
    return model_fit


# 데이터 전처리
def _to_dataframe(data: Iterable[sqlite3.Row]):
    df = pd.DataFrame(data)

    df['date_created'] = pd.to_datetime(df['date_created'])
    df = df.astype({
        'cpu_usage': 'float', 'dsk_usage': 'float', 'mem_usage': 'float',
        'gpu_usage': 'float', 'dsk_io': 'float'
    })

    df.set_index('date_created', inplace=True)
    # df.dropna(axis=0, inplace=True)
    df.fillna(0, inplace=True)

    return df
